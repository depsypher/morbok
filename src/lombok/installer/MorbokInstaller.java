/*
 * $Id$
 * $URL$
 */
package lombok.installer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import lombok.installer.EclipseFinder.OS;
import lombok.installer.EclipseLocation.InstallException;
import lombok.installer.EclipseLocation.NotAnEclipseException;
import lombok.installer.EclipseLocation.UninstallException;
import morbok.core.Version;

/**
 * The morbok installer ripped off as much as possible from the good folks at projectlombok.
 * Uses swing to show a simple GUI that can add and remove the java agent to Eclipse installations.
 * Also offers info on what this installer does in case people want to instrument their Eclipse manually,
 * and looks in some common places on Mac OS X and Windows.
 */
public class MorbokInstaller {
	private static final URI ABOUT_MORBOK_URL = URI.create("http://code.google.com/p/morbok/");

	private final JFrame appWindow;

	private JComponent loadingExpl;

	private Component javacArea;
	private Component eclipseArea;
	private Component uninstallArea;
	private Component howIWorkArea;

	private Box uninstallBox;
	private List<EclipseLocation> toUninstall;

	private JHyperLink uninstallButton;
	private JLabel uninstallPlaceholder;
	private JButton installButton;

	public static void main(String[] args) {
		if (args.length > 0 && args[0].equals("install")) {
			if (args.length < 3 || !args[1].equals("eclipse")) {
				System.err.println("Run java -jar morbok.jar install eclipse path/to/eclipse/executable");
				System.exit(1);
			}
			String path = args[2];
			try {
				EclipseLocation loc = EclipseLocation.create(path);
				loc.install();
				System.out.println("Installed to: " + loc.getName());
				System.exit(0);
			} catch (NotAnEclipseException e) {
				System.err.println("Not a valid eclipse location:");
				System.err.println(e.getMessage());
				System.exit(2);
			} catch (InstallException e) {
				System.err.println("Installation failed:");
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}

		if (args.length > 0 && args[0].equals("uninstall")) {
			if (args.length < 3 || !args[1].equals("eclipse")) {
				System.err.println("Run java -jar morbok.jar uninstall eclipse path/to/eclipse/executable");
				System.exit(1);
			}
			String path = args[2];
			try {
				EclipseLocation loc = EclipseLocation.create(path);
				loc.uninstall();
				System.out.println("Uninstalled from: " + loc.getName());
				System.exit(0);
			} catch (NotAnEclipseException e) {
				System.err.println("Not a valid eclipse location:");
				System.err.println(e.getMessage());
				System.exit(2);
			} catch (UninstallException e) {
				System.err.println("Uninstall failed:");
				System.err.println(e.getMessage());
				System.exit(1);
			}
		}

		if (EclipseFinder.getOS() == OS.MAC_OS_X) {
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Morbok Installer");
			System.setProperty("com.apple.macos.use-file-dialog-packages", "true");
		}

		try {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
						} catch (Exception ignore) {}

						new MorbokInstaller().show();
					} catch (HeadlessException e) {
						printHeadlessInfo();
					}
				}
			});
		} catch (HeadlessException e) {
			printHeadlessInfo();
		}
	}

	/**
	 * If run in headless mode, the installer can't show its fancy GUI. There's little point in running
	 * the installer without a GUI environment, as Eclipse doesn't run in headless mode either, so
	 * we'll make do with showing some basic info on Morbok as well as instructions for using morbok with javac.
	 */
	private static void printHeadlessInfo() {
		System.out.printf("About morbok v%s\n" +
				"Morbok makes java better by providing very spicy additions to Lombok," +
				"such as using @Logger to automatically generate a logger for your class.\n\n" +
				"Browse to %s for more information. To install morbok on Eclipse, re-run this jar file on a " +
				"graphical computer system - this message is being shown because your terminal is not graphics capable." +
				"If you are just using 'javac' or a tool that calls on javac, no installation is neccessary; just " +
				"make sure morbok.jar is in the classpath along with lombok.jar when you compile. Example:\n\n" +
				"   java -cp lombok.jar:morbok.jar MyCode.java\n\n\n" +
				"If for whatever reason you can't run the graphical installer but you do want to install morbok into eclipse," +
				"start this jar with the following syntax:\n\n" +
				"   java -jar morbok.jar install eclipse path/to/your/eclipse/executable", Version.getVersion(), ABOUT_MORBOK_URL);
	}

	/**
	 * Creates a new installer that starts out invisible.
	 * Call the {@link #show()} method on a freshly created installer to render it.
	 */
	public MorbokInstaller() {
		appWindow = new JFrame(String.format("Project Morbok v%s - Installer", Version.getVersion()));

		appWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		appWindow.setResizable(false);
		appWindow.setIconImage(Toolkit.getDefaultToolkit().getImage(MorbokInstaller.class.getResource("lombokIcon.png")));

		try {
			javacArea = this.buildJavacArea();
			eclipseArea = this.buildEclipseArea();
			uninstallArea = this.buildUninstallArea();
			uninstallArea.setVisible(false);
			howIWorkArea = this.buildHowIWorkArea();
			howIWorkArea.setVisible(false);
			this.buildChrome(appWindow.getContentPane());
			appWindow.pack();
		} catch (Throwable t) {
			this.handleException(t);
		}
	}

	private void handleException(final Throwable t) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override public void run() {
				JOptionPane.showMessageDialog(appWindow, "There was a problem during the installation process:\n" + t, "Uh Oh!", JOptionPane.ERROR_MESSAGE);
				t.printStackTrace();
				System.exit(1);
			}
		});
	}

	private Component buildHowIWorkArea() {
		JPanel container = new JPanel();

		container.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;

		container.add(new JLabel(HOW_I_WORK_TITLE), constraints);

		constraints.gridy = 1;
		constraints.insets = new Insets(8, 0, 0, 16);
		container.add(new JLabel(String.format(HOW_I_WORK_EXPLANATION, File.pathSeparator)), constraints);

		Box buttonBar = Box.createHorizontalBox();
		JButton backButton = new JButton("Okay - Good to know!");
		buttonBar.add(Box.createHorizontalGlue());
		buttonBar.add(backButton);

		backButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				howIWorkArea.setVisible(false);
				javacArea.setVisible(true);
				eclipseArea.setVisible(true);
				appWindow.pack();
			}
		});

		constraints.gridy = 2;
		container.add(buttonBar, constraints);

		return container;
	}

	private Component buildUninstallArea() {
		JPanel container = new JPanel();

		container.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;

		container.add(new JLabel(UNINSTALL_TITLE), constraints);

		constraints.gridy = 1;
		constraints.insets = new Insets(8, 0, 0, 16);
		container.add(new JLabel(UNINSTALL_EXPLANATION), constraints);

		uninstallBox = Box.createVerticalBox();
		constraints.gridy = 2;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		container.add(uninstallBox, constraints);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridy = 3;
		container.add(new JLabel("Are you sure?"), constraints);

		Box buttonBar = Box.createHorizontalBox();
		JButton noButton = new JButton("No - Don't uninstall");
		buttonBar.add(noButton);
		buttonBar.add(Box.createHorizontalGlue());
		JButton yesButton = new JButton("Yes - uninstall Morbok");
		buttonBar.add(yesButton);

		noButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				uninstallArea.setVisible(false);
				javacArea.setVisible(true);
				eclipseArea.setVisible(true);
				appWindow.pack();
			}
		});

		yesButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				MorbokInstaller.this.doUninstall();
			}
		});

		constraints.gridy = 4;
		container.add(buttonBar, constraints);

		return container;
	}

	private Component buildJavacArea() {
		JPanel container = new JPanel();

		container.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(8, 0, 0, 16);

		container.add(new JLabel(JAVAC_TITLE), constraints);

		constraints.gridy = 1;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		container.add(new JLabel(JAVAC_EXPLANATION), constraints);

		JLabel example = new JLabel(JAVAC_EXAMPLE);

		constraints.gridy = 2;
		container.add(example, constraints);
		return container;
	}

	private Component buildEclipseArea() {
		JPanel container = new JPanel();

		container.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;

		constraints.insets = new Insets(8, 0, 0, 16);
		container.add(new JLabel(ECLIPSE_TITLE), constraints);

		constraints.gridy = 1;
		container.add(new JLabel(ECLIPSE_EXPLANATION), constraints);

		constraints.gridy = 2;
		loadingExpl = Box.createHorizontalBox();
		loadingExpl.add(new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/loading.gif"))));
		loadingExpl.add(new JLabel(ECLIPSE_LOADING_EXPLANATION));
		container.add(loadingExpl, constraints);

		constraints.weightx = 1.0;
		constraints.gridy = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		eclipsesList = new EclipsesList();

		JScrollPane eclipsesListScroll = new JScrollPane(eclipsesList);
		eclipsesListScroll.setBackground(Color.WHITE);
		eclipsesListScroll.getViewport().setBackground(Color.WHITE);
		container.add(eclipsesListScroll, constraints);

		Thread findEclipsesThread = new Thread() {
			@Override public void run() {
				try {
					final List<EclipseLocation> locations = new ArrayList<EclipseLocation>();
					final List<NotAnEclipseException> problems = new ArrayList<NotAnEclipseException>();
					EclipseFinder.findEclipses(locations, problems);

					SwingUtilities.invokeLater(new Runnable() {
						@Override public void run() {
							for (EclipseLocation location : locations) {
								try {
									eclipsesList.addEclipse(location);
								} catch (Throwable t) {
									MorbokInstaller.this.handleException(t);
								}
							}

							for (NotAnEclipseException problem : problems) {
								problem.showDialog(appWindow);
							}

							loadingExpl.setVisible(false);

							if (locations.size() + problems.size() == 0) {
								JOptionPane.showMessageDialog(appWindow,
										"I don't know how to automatically find Eclipse installations on this platform.\n" +
										"Please use the 'Specify Eclipse Location...' button to manually point out the\n" +
										"location of your Eclipse installation to me. Thanks!", "Can't find Eclipse", JOptionPane.INFORMATION_MESSAGE);
							}
						}
					});
				} catch (Throwable t) {
					MorbokInstaller.this.handleException(t);
				}
			}
		};

		findEclipsesThread.start();

		Box buttonBar = Box.createHorizontalBox();
		JButton specifyEclipseLocationButton = new JButton("Specify Eclipse location...");
		buttonBar.add(specifyEclipseLocationButton);
		specifyEclipseLocationButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				final String exeName = EclipseFinder.getEclipseExecutableName();
				String file = null;

				if (EclipseFinder.getOS() == OS.MAC_OS_X) {
					FileDialog chooser = new FileDialog(appWindow);
					chooser.setMode(FileDialog.LOAD);
					chooser.setFilenameFilter(new FilenameFilter() {
						@Override public boolean accept(File dir, String fileName) {
							if (exeName.equalsIgnoreCase(fileName)) return true;
							if (new File(dir, fileName).isDirectory()) return true;
							return false;
						}
					});

					chooser.setVisible(true);
					file = new File(chooser.getDirectory(), chooser.getFile()).getAbsolutePath();
				} else {
					JFileChooser chooser = new JFileChooser();

					chooser.setAcceptAllFileFilterUsed(false);
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					chooser.setFileFilter(new FileFilter() {
						@Override public boolean accept(File f) {
							if (f.getName().equalsIgnoreCase(exeName)) return true;
							if (f.getName().equalsIgnoreCase("eclipse.ini")) return true;
							if (f.isDirectory()) return true;

							return false;
						}

						@Override public String getDescription() {
							return "Eclipse Installation";
						}
					});

					switch (chooser.showDialog(appWindow, "Select")) {
					case JFileChooser.APPROVE_OPTION:
						file = chooser.getSelectedFile().getAbsolutePath();
					}
				}

				if (file != null) {
					try {
						eclipsesList.addEclipse(EclipseLocation.create(file));
					} catch (NotAnEclipseException e) {
						e.showDialog(appWindow);
					} catch (Throwable t) {
						MorbokInstaller.this.handleException(t);
					}
				}
			}
		});

		buttonBar.add(Box.createHorizontalGlue());
		installButton = new JButton("Install / Update");
		buttonBar.add(installButton);

		installButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				List<EclipseLocation> locationsToInstall = new ArrayList<EclipseLocation>(eclipsesList.getSelectedEclipses());
				if (locationsToInstall.isEmpty()) {
					JOptionPane.showMessageDialog(appWindow, "You haven't selected any Eclipse installations!.", "No Selection", JOptionPane.WARNING_MESSAGE);
					return;
				}

				MorbokInstaller.this.install(locationsToInstall);
			}
		});

		constraints.gridy = 4;
		constraints.weightx = 0;
		container.add(buttonBar, constraints);

		constraints.gridy = 5;
		constraints.fill = GridBagConstraints.NONE;
		JHyperLink showMe = new JHyperLink("Show me what this installer will do to my Eclipse installation.");
		container.add(showMe, constraints);
		showMe.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				MorbokInstaller.this.showWhatIDo();
			}
		});

		constraints.gridy = 6;
		uninstallButton = new JHyperLink("Uninstall morbok from selected Eclipse installations.");
		uninstallPlaceholder = new JLabel("<html>&nbsp;</html>");
		uninstallButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				List<EclipseLocation> locationsToUninstall = new ArrayList<EclipseLocation>();
				for (EclipseLocation location : eclipsesList.getSelectedEclipses()) {
					if (location.hasLombok()) locationsToUninstall.add(location);
				}

				if (locationsToUninstall.isEmpty()) {
					JOptionPane.showMessageDialog(appWindow, "You haven't selected any Eclipse installations that have been lombok-enabled.", "No Selection", JOptionPane.WARNING_MESSAGE);
					return;
				}


				MorbokInstaller.this.uninstall(locationsToUninstall);
			}
		});
		container.add(uninstallButton, constraints);
		uninstallPlaceholder.setVisible(false);
		container.add(uninstallPlaceholder, constraints);


		return container;
	}

	private void showWhatIDo() {
		javacArea.setVisible(false);
		eclipseArea.setVisible(false);
		howIWorkArea.setVisible(true);
		appWindow.pack();
	}

	private void uninstall(List<EclipseLocation> locations) {
		javacArea.setVisible(false);
		eclipseArea.setVisible(false);

		uninstallBox.removeAll();
		uninstallBox.add(Box.createRigidArea(new Dimension(1, 16)));
		for (EclipseLocation location : locations) {
			JLabel label = new JLabel(location.getName());
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			uninstallBox.add(label);
		}
		uninstallBox.add(Box.createRigidArea(new Dimension(1, 16)));

		toUninstall = locations;
		uninstallArea.setVisible(true);
		appWindow.pack();
	}

	private void install(final List<EclipseLocation> toInstall) {
		JPanel spinner = new JPanel();
		spinner.setOpaque(true);
		spinner.setLayout(new FlowLayout());
		spinner.add(new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/loading.gif"))));
		appWindow.setContentPane(spinner);

		final AtomicReference<Boolean> success = new AtomicReference<Boolean>(true);

		new Thread() {
			@Override public void run() {
				for (EclipseLocation loc : toInstall) {
					try {
						loc.install();
					} catch (final InstallException e) {
						success.set(false);
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override public void run() {
									JOptionPane.showMessageDialog(appWindow,
											e.getMessage(), "Install Problem", JOptionPane.ERROR_MESSAGE);
								}
							});
						} catch (Exception e2) {
							//Shouldn't happen.
							throw new RuntimeException(e2);
						}
					}
				}

				if (success.get()) SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						JOptionPane.showMessageDialog(appWindow,
								"<html>Morbok has been installed on the selected Eclipse installations.<br>" +
								"Don't forget to add <code>morbok.jar</code> to your projects, and restart your eclipse!<br>" +
								"If you start eclipse with a custom -vm parameter, you'll need to add:<br>" +
								"<code>-vmargs -Xbootclasspath/a:lombok.jar" + File.pathSeparator + "morbok.jar -javaagent:lombok.jar</code><br>" +
								"as parameter as well.</html>", "Install successful",
								JOptionPane.INFORMATION_MESSAGE);
						appWindow.setVisible(false);
						System.exit(0);
					}
				});

				if (!success.get()) SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						System.exit(0);
					}
				});
			}
		}.start();
	}

	private void doUninstall() {
		JPanel spinner = new JPanel();
		spinner.setOpaque(true);
		spinner.setLayout(new FlowLayout());
		spinner.add(new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/loading.gif"))));

		appWindow.setContentPane(spinner);

		final AtomicReference<Boolean> success = new AtomicReference<Boolean>(true);
		new Thread() {
			@Override public void run() {
				for (EclipseLocation loc : toUninstall) {
					try {
						loc.uninstall();
					} catch (final UninstallException e) {
						success.set(false);
						try {
							SwingUtilities.invokeAndWait(new Runnable() {
								@Override public void run() {
									JOptionPane.showMessageDialog(appWindow,
											e.getMessage(), "Uninstall Problem", JOptionPane.ERROR_MESSAGE);
								}
							});
						} catch (Exception e2) {
							//Shouldn't happen.
							throw new RuntimeException(e2);
						}
					}
				}

				if (success.get()) SwingUtilities.invokeLater(new Runnable() {
					@Override public void run() {
						JOptionPane.showMessageDialog(appWindow, "Morbok has been removed from the selected Eclipse installations.", "Uninstall successful", JOptionPane.INFORMATION_MESSAGE);
						appWindow.setVisible(false);
						System.exit(0);
					}
				});
			}
		}.start();
	}

	private EclipsesList eclipsesList = new EclipsesList();

	private static class JHyperLink extends JButton {
		private static final long serialVersionUID = 1L;

		public JHyperLink(String text) {
			super();
			this.setFont(this.getFont().deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, 1)));
			this.setText(text);
			this.setBorder(null);
			this.setContentAreaFilled(false);
			this.setForeground(Color.BLUE);
			this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			this.setMargin(new Insets(0, 0, 0, 0));
		}
	}

	void selectedLomboksChanged(List<EclipseLocation> selectedEclipses) {
		boolean uninstallAvailable = false;
		boolean installAvailable = false;
		for (EclipseLocation loc : selectedEclipses) {
			if (loc.hasLombok()) uninstallAvailable = true;
			installAvailable = true;
		}

		uninstallButton.setVisible(uninstallAvailable);
		uninstallPlaceholder.setVisible(!uninstallAvailable);
		installButton.setEnabled(installAvailable);
	}

	private class EclipsesList extends JPanel implements Scrollable {
		private static final long serialVersionUID = 1L;

		List<EclipseLocation> locations = new ArrayList<EclipseLocation>();

		EclipsesList() {
			this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			this.setBackground(Color.WHITE);
		}

		List<EclipseLocation> getSelectedEclipses() {
			List<EclipseLocation> list = new ArrayList<EclipseLocation>();
			for (EclipseLocation loc : locations) if (loc.selected) list.add(loc);
			return list;
		}

		void fireSelectionChange() {
			MorbokInstaller.this.selectedLomboksChanged(this.getSelectedEclipses());
		}

		void addEclipse(final EclipseLocation location) {
			if (locations.contains(location)) return;
			Box box = Box.createHorizontalBox();
			box.setBackground(Color.WHITE);
			final JCheckBox checkbox = new JCheckBox(location.getName());
			checkbox.setBackground(Color.WHITE);
			box.add(checkbox);
			checkbox.addActionListener(new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					location.selected = checkbox.isSelected();
					EclipsesList.this.fireSelectionChange();
				}
			});

			if (location.hasLombok()) {
				checkbox.setSelected(true);
				box.add(new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/realLombokIcon.png"))));
			}
			else {
				JLabel label = new JLabel("(Lombok required)");
				label.setForeground(Color.RED);
				box.add(label);
				checkbox.setSelected(false);
				checkbox.setEnabled(false);
				location.selected = false;
			}
			if (location.hasMorbok()) {
				box.add(new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/lombokIcon.png"))));
			}

			box.add(Box.createHorizontalGlue());
			locations.add(location);
			this.add(box);
			this.getParent().doLayout();
			this.fireSelectionChange();
		}

		@Override public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(1, 100);
		}

		@Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 12;
		}

		@Override public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		@Override public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return 1;
		}
	}

	private void buildChrome(Container appWindowContainer) {
		JLabel leftGraphic = new JLabel(new ImageIcon(MorbokInstaller.class.getResource("/lombok/installer/morbok.png")));

		GridBagConstraints constraints = new GridBagConstraints();

		appWindowContainer.setLayout(new GridBagLayout());

		constraints.gridheight = 3;
		constraints.gridwidth = 1;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(8, 8, 8, 8);
		appWindowContainer.add(leftGraphic,constraints);
		constraints.insets = new Insets(0, 0, 0, 0);

		constraints.gridx++;
		constraints.gridy++;
		constraints.gridheight = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.ipadx = 16;
		constraints.ipady = 14	;
		appWindowContainer.add(javacArea, constraints);

		constraints.gridy++;
		appWindowContainer.add(eclipseArea, constraints);

		appWindowContainer.add(uninstallArea, constraints);

		appWindowContainer.add(howIWorkArea, constraints);

		constraints.gridy++;
		constraints.gridwidth = 2;
		constraints.gridx = 0;
		constraints.weightx = 0;
		constraints.weighty = 0;
		constraints.ipadx = 0;
		constraints.ipady = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.SOUTHEAST;
		constraints.insets = new Insets(0, 16, 8, 8);
		Box buttonBar = Box.createHorizontalBox();
		JButton quitButton = new JButton("Quit Installer");
		quitButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent e) {
				appWindow.setVisible(false);
				System.exit(0);
			}
		});
		final JHyperLink hyperlink = new JHyperLink(ABOUT_MORBOK_URL.toString());
		hyperlink.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent event) {
				hyperlink.setForeground(new Color(85, 145, 90));
				try {
					//java.awt.Desktop doesn't exist in 1.5.
					Object desktop = Class.forName("java.awt.Desktop").getMethod("getDesktop").invoke(null);
					Class.forName("java.awt.Desktop").getMethod("browse", URI.class).invoke(desktop, ABOUT_MORBOK_URL);
				} catch (Exception e) {
					Runtime rt = Runtime.getRuntime();
					try {
						switch (EclipseFinder.getOS()) {
						case WINDOWS:
							String[] cmd = new String[4];
							cmd[0] = "cmd.exe";
							cmd[1] = "/C";
							cmd[2] = "start";
							cmd[3] = ABOUT_MORBOK_URL.toString();
							rt.exec(cmd);
							break;
						case MAC_OS_X:
							rt.exec( "open " + ABOUT_MORBOK_URL.toString());
							break;
						default:
						case UNIX:
							rt.exec("firefox " + ABOUT_MORBOK_URL.toString());
							break;
						}
					} catch (Exception e2) {
						JOptionPane.showMessageDialog(appWindow,
								"Well, this is embarrassing. I don't know how to open a webbrowser.\n" +
								"I guess you'll have to open it. Browse to:\n" + ABOUT_MORBOK_URL +
								"for more information about Morbok.",
								"I'm embarrassed", JOptionPane.INFORMATION_MESSAGE);
					}
				}
			}
		});
		buttonBar.add(hyperlink);
		buttonBar.add(Box.createRigidArea(new Dimension(16, 1)));
		buttonBar.add(new JLabel("<html><font size=\"-1\">v" + Version.getVersion() + "</font></html>"));

		buttonBar.add(Box.createHorizontalGlue());
		buttonBar.add(quitButton);
		appWindow.add(buttonBar, constraints);
	}

	/**
	 * Makes the installer window visible.
	 */
	public void show() {
		appWindow.setVisible(true);
		if (EclipseFinder.getOS() == OS.MAC_OS_X) {
			try {
				AppleNativeLook.go();
			} catch (Throwable ignore) {
				//We're just prettying up the app. If it fails, meh.
			}
		}
	}

	private static final String ECLIPSE_TITLE =
		"<html><font size=\"+1\"><b><i>Eclipse</i></b></font></html>";

	private static final String ECLIPSE_EXPLANATION =
		"<html>Morbok can update your Eclipse to fully support all Morbok features.<br>" +
		"Select Eclipse installations below and hit 'Install/Update'.</html>";

	private static final String ECLIPSE_LOADING_EXPLANATION =
		"Scanning your drives for Eclipse installations...";

	private static final String JAVAC_TITLE =
		"<html><font size=\"+1\"><b><i>Javac</i></b></font> &nbsp;&nbsp;&nbsp;&nbsp;&nbsp; (and tools that invoke javac such as <i>ant</i> and <i>maven</i>)</html>";

	private static final String JAVAC_EXPLANATION =
		"<html>Morbok works 'out of the box' with javac.<br>Just make sure the lombok.jar and morbok.jar are in your classpath when you compile.";

	private static final String JAVAC_EXAMPLE =
		"<html>Example: <code>javac -cp lombok.jar" + File.pathSeparator + "morbok.jar MyCode.java</code></html>";

	private static final String UNINSTALL_TITLE =
		"<html><font size=\"+1\"><b><i>Uninstall</i></b></font></html>";

	private static final String UNINSTALL_EXPLANATION =
		"<html>Uninstall Morbok from the following Eclipse Installations?</html>";

	private static final String HOW_I_WORK_TITLE =
		"<html><font size=\"+1\"><b><i>What this installer does</i></b></font></html>";

	private static final String HOW_I_WORK_EXPLANATION =
		"<html><ol>" +
		"<li>First, I copy myself (morbok.jar) to your Eclipse install directory.</li>" +
		"<li>Then, I edit the eclipse.ini file to add the following two entries:<br>" +
		"<pre>-Xbootclasspath/a:lombok.jar:morbok.jar<br>" +
		"-javaagent:lombok.jar</pre></li></ol>" +
		"<br>" +
		"That's all there is to it. Note that on Mac OS X, eclipse.ini is hidden in<br>" +
		"<code>Eclipse.app%1$sContents%1$sMacOS</code> so that's where I place the jar files.</html>";
}
