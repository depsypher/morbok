/*
 * $Id$
 * $URL$
 */
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.lang.reflect.Modifier;

import lombok.core.AnnotationValues;
import lombok.eclipse.Eclipse;
import lombok.eclipse.EclipseAnnotationHandler;
import lombok.eclipse.EclipseNode;
import lombok.eclipse.handlers.EclipseHandlerUtil.MemberExistsResult;
import morbok.Logger;

import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.MessageSend;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
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
    public boolean handle(AnnotationValues<Logger> annotation, Annotation source, EclipseNode annotationNode)
    {
        EclipseNode typeNode = annotationNode.up();

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
        long pos = (long)typeDecl.sourceStart << 32 | typeDecl.sourceEnd;
        int pS = (int)(pos >> 32), pE = (int)pos;

        String logName = typeNode.getPackageDeclaration() + "." + typeNode.getName();
        char[] logVariableName = this.getLogName(annotation);

        if (logVariableName != null && fieldExists(new String(logVariableName), typeNode) == MemberExistsResult.NOT_EXISTS)
        {
            FieldDeclaration fieldDecl = new FieldDeclaration(logVariableName, 0, -1);
            Eclipse.setGeneratedBy(fieldDecl, source);

            fieldDecl.modifiers = (Modifier.STATIC | Modifier.FINAL | Modifier.PRIVATE);

//            InputStream in = this.getClass().getResourceAsStream("logger.properties");
//            Properties props = new Properties();
//            try
//            {
//                if (in != null)
//                    props.load(in);
//            }
//            catch(IOException e)
//            {
//                e.printStackTrace();
//            }
//            Eclipse.error(null, props.getProperty("foo", "not found"));

            switch (annotation.getInstance().type())
            {
                case JAVA:
                    fieldDecl.type = new QualifiedTypeReference(
                            Eclipse.fromQualifiedName("java.util.logging.Logger"),
                            new long[] { pos, pos, pos, pos });

                    Eclipse.error(null, "java util");
                    break;

                case LOG4J:
                    fieldDecl.type = new QualifiedTypeReference(
                            Eclipse.fromQualifiedName("org.apache.commons.logging.Log"),
                            new long[] { pos, pos, pos, pos, pos });

                    Eclipse.error(null, "log4j");
                    break;

                default:
                    throw new IllegalStateException("Got an unexpected Logger type: " + annotation.getInstance().type());
            }
            Eclipse.setGeneratedBy(fieldDecl.type, source);

            MessageSend send = new MessageSend();
            Eclipse.setGeneratedBy(send, source);

            switch (annotation.getInstance().type())
            {
                case JAVA:
                    send.receiver = new QualifiedNameReference(
                            Eclipse.fromQualifiedName("java.util.logging.Logger"),
                            new long[] { pos, pos, pos, pos, pos }, pS, pE);

                    send.selector = "getLogger".toCharArray();
                    Eclipse.error(null, "java util");
                    break;

                case LOG4J:
                    send.receiver = new QualifiedNameReference(
                            Eclipse.fromQualifiedName("org.apache.commons.logging.LogFactory"),
                            new long[] { pos, pos, pos, pos, pos }, pS, pE);

                    send.selector = "getLog".toCharArray();
                    Eclipse.error(null, "log4j");
                    break;

                default:
                    throw new IllegalStateException("Got an unexpected Logger type: " + annotation.getInstance().type());
            }

            send.receiver.statementEnd = pE;
            Eclipse.setGeneratedBy(send.receiver, source);

            Expression arg = new StringLiteral(logName.toCharArray(), 0, 0, 0);
            Eclipse.setGeneratedBy(arg, source);
            arg.statementEnd = pE;
            send.arguments = new Expression[] { arg };

            send.nameSourcePosition = pos;
            send.sourceStart = pS;
            send.sourceEnd = send.statementEnd = pE;

            fieldDecl.initialization = send;

            injectField(typeNode, fieldDecl);
        }

        return true;
    }

    /* */
    private char[] getLogName(AnnotationValues<Logger> annotation)
    {
        String name = annotation.getInstance().name();

        if (name == null || !name.matches("^[^0-9][a-zA-Z0-9$]*$"))
            return null;

        return name.toCharArray();
    }
}
