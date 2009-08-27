/*
 * $Id$
 * $URL$
 */
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.PKG.*;

import java.lang.reflect.Modifier;

import lombok.core.AnnotationValues;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseAST.Node;
import lombok.eclipse.handlers.PKG.MemberExistsResult;
import morbok.Logger;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.mangosdk.spi.ProviderFor;

/**
 * Handles the <code>morbok.Logger</code> annotation for eclipse.
 *
 * @author rayvanderborght
 */
@ProviderFor(EclipseAnnotationHandler.class)
public class HandleLogger implements EclipseAnnotationHandler<Logger>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handle(AnnotationValues<Logger> annotation, Annotation ast, Node annotationNode)
    {
        Node typeNode = annotationNode.up();

        TypeDeclaration typeDecl = null;
        if (typeNode.get() instanceof TypeDeclaration)
            typeDecl = (TypeDeclaration) typeNode.get();

        int modifiers = typeDecl == null ? 0 : typeDecl.modifiers;
        boolean notAClass = (modifiers &
                (ClassFileConstants.AccInterface | ClassFileConstants.AccAnnotation | ClassFileConstants.AccEnum)) != 0;

        if (typeDecl == null || notAClass)
        {
            annotationNode.addError("@Logger is only supported on a class.");
            return false;
        }

        String logName = typeNode.getPackageDeclaration() + "." + typeNode.getName();
        char[] logVariableName = this.getLogName(annotation);

        if (logVariableName != null && fieldExists(new String(logVariableName), typeNode) == MemberExistsResult.NOT_EXISTS)
        {
            FieldDeclaration fieldDecl = new FieldDeclaration("BOGUS_FIELD".toCharArray(), 0, -1);
            fieldDecl.declarationSourceEnd = -1;
            fieldDecl.modifiers = (Modifier.STATIC);
            fieldDecl.type = new QualifiedTypeReference(TypeConstants.JAVA_LANG_OBJECT, new long[] { 0, 0, 0 });

            /*
             * Why inject this bogus field you ask?  Currently it's the only
             * way I know of stopping the following exception from happening.
             * Why does it help to inject a do-nothing field first?  I have
             * no idea... there must be a better way.
             *
             * java.lang.ArrayIndexOutOfBoundsException: 4
             *  at org.eclipse.jdt.core.dom.ASTConverter.convertType(ASTConverter.java:3333)
             *  at org.eclipse.jdt.core.dom.ASTConverter.convertToFieldDeclaration(ASTConverter.java:2916)
             *  at org.eclipse.jdt.core.dom.ASTConverter.checkAndAddMultipleFieldDeclaration(ASTConverter.java:376)
             *  at org.eclipse.jdt.core.dom.ASTConverter.buildBodyDeclarations(ASTConverter.java:172)
             *  at org.eclipse.jdt.core.dom.ASTConverter.convert(ASTConverter.java:2694)
             *  at org.eclipse.jdt.core.dom.ASTConverter.convert(ASTConverter.java:1264)
             *  at org.eclipse.jdt.core.dom.CompilationUnitResolver.convert(CompilationUnitResolver.java:256)
             *  at org.eclipse.jdt.core.dom.ASTParser.internalCreateAST(ASTParser.java:933)
             *  at org.eclipse.jdt.core.dom.ASTParser.createAST(ASTParser.java:657)
             *  at org.eclipse.jdt.internal.ui.javaeditor.ASTProvider$1.run(ASTProvider.java:544)
             *  at org.eclipse.core.runtime.SafeRunner.run(SafeRunner.java:42)
             *  at org.eclipse.jdt.internal.ui.javaeditor.ASTProvider.createAST(ASTProvider.java:537)
             *  at org.eclipse.jdt.internal.ui.javaeditor.ASTProvider.getAST(ASTProvider.java:478)
             *  at org.eclipse.jdt.ui.SharedASTProvider.getAST(SharedASTProvider.java:126)
             *  at org.eclipse.jdt.internal.ui.viewsupport.SelectionListenerWithASTManager$PartListenerGroup.calculateASTandInform(SelectionListenerWithASTManager.java:169)
             *  at org.eclipse.jdt.internal.ui.viewsupport.SelectionListenerWithASTManager$3.run(SelectionListenerWithASTManager.java:154)
             *  at org.eclipse.core.internal.jobs.Worker.run(Worker.java:55)
             */
            injectField(typeNode, fieldDecl);

//            FieldDeclaration fieldDecl = new FieldDeclaration(logVariableName, 0, -1);
            fieldDecl = new FieldDeclaration(logVariableName, 0, -1);

            fieldDecl.declarationSourceEnd = -1;
            fieldDecl.modifiers = (Modifier.STATIC | Modifier.FINAL | Modifier.PRIVATE);

            fieldDecl.type = new QualifiedTypeReference(
                    new char[][] { "org".toCharArray(), "apache".toCharArray(), "commons".toCharArray(), "logging".toCharArray(), "Log".toCharArray() },
                    new long[] { 0, 0, 0 });

            MessageSend send = new MessageSend();
            send.receiver = this.generateQualifiedNameRef(new char[][] { "org".toCharArray(), "apache".toCharArray(), "commons".toCharArray(), "logging".toCharArray(), "LogFactory".toCharArray() });
            send.arguments = new Expression[] { new StringLiteral(logName.toCharArray(), 0, 0, 0) };
            send.selector = "getLog".toCharArray();

            fieldDecl.initialization = send;

            injectField(typeNode, fieldDecl);
        }

        return true;
    }

    /* */
    private char[] getLogName(AnnotationValues<Logger> annotation)
    {
        String name = annotation.getInstance().name();

        if (name == null || !name.matches("[a-zA-Z0-9$]*"))
            return null;

        return name.toCharArray();
    }

    /* */
    private NameReference generateQualifiedNameRef(char[]... varNames)
    {
        return (varNames.length > 1)
            ? new QualifiedNameReference(varNames, new long[varNames.length], 0, 0)
            : new SingleNameReference(varNames[0], 0);
    }
}
