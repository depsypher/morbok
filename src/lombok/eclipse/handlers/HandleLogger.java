/*
 * $Id$
 * $URL$
 */
package lombok.eclipse.handlers;

import static lombok.eclipse.handlers.EclipseHandlerUtil.*;

import java.lang.reflect.Modifier;

import lombok.core.AnnotationValues;
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
    public boolean handle(AnnotationValues<Logger> annotation, Annotation ast, EclipseNode annotationNode)
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

            fieldDecl.modifiers = (Modifier.STATIC | Modifier.FINAL | Modifier.PRIVATE);

            fieldDecl.type = new QualifiedTypeReference(
                    new char[][] { "org".toCharArray(), "apache".toCharArray(), "commons".toCharArray(), "logging".toCharArray(), "Log".toCharArray() },
                    new long[] { pos, pos, pos, pos, pos });

            MessageSend send = new MessageSend();
            send.receiver = new QualifiedNameReference(
                    new char[][] { "org".toCharArray(), "apache".toCharArray(), "commons".toCharArray(), "logging".toCharArray(), "LogFactory".toCharArray() },
                    new long[] { pos, pos, pos, pos, pos }, pS, pE);

            send.arguments = new Expression[] { new StringLiteral(logName.toCharArray(), 0, 0, 0) };
            send.selector = "getLog".toCharArray();
            send.sourceStart = pS;
            send.sourceEnd = pE;
            send.statementEnd = pE;

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
}
