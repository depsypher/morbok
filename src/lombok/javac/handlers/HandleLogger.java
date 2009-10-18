/*
 * $Id$
 * $URL$
 */
package lombok.javac.handlers;

import static lombok.javac.handlers.PKG.*;
import lombok.core.AnnotationValues;
import lombok.javac.JavacAnnotationHandler;
import lombok.javac.JavacNode;
import lombok.javac.handlers.PKG.MemberExistsResult;
import morbok.Logger;

import org.mangosdk.spi.ProviderFor;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.TypeTags;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Handles the <code>morbok.Logger</code> annotation for javac.
 *
 * @author rayvanderborght
 */
@ProviderFor(JavacAnnotationHandler.class)
public class HandleLogger implements JavacAnnotationHandler<Logger>
{
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handle(AnnotationValues<Logger> annotation, JCAnnotation ast, JavacNode annotationNode)
    {
        JavacNode typeNode = annotationNode.up();

        JCClassDecl typeDecl = null;
        if (typeNode.get() instanceof JCClassDecl)
            typeDecl = (JCClassDecl)typeNode.get();

        long flags = typeDecl == null ? 0 : typeDecl.mods.flags;
        boolean notAClass = (flags & (Flags.INTERFACE | Flags.ENUM | Flags.ANNOTATION)) != 0;

        if (typeDecl == null || notAClass)
        {
            annotationNode.addError("@Logger is only supported on a class.");
            return false;
        }

        String logVariableName = this.getLogName(annotation);
        TreeMaker maker = typeNode.getTreeMaker();

        if (logVariableName != null && fieldExists(logVariableName, typeNode) == MemberExistsResult.NOT_EXISTS)
        {
            JCExpression objectType = chainDots(maker, typeNode, "org", "apache", "commons", "logging", "Log");
            JCExpression logFactory = chainDots(maker, typeNode, "org", "apache", "commons", "logging", "LogFactory", "getLog");

            //argument list for method
            ListBuffer<JCExpression> args = new ListBuffer<JCExpression>();

            JCLiteral literal = maker.Literal(TypeTags.CLASS, typeDecl.sym.type.toString());
            args.append(literal);

            JCExpression logValue = maker.Apply(List.<JCExpression> nil(), logFactory, args.toList());

            JCVariableDecl fieldDecl = maker.VarDef(
                    maker.Modifiers(Flags.PRIVATE | Flags.FINAL | Flags.STATIC),
                    typeNode.toName(logVariableName), objectType, logValue);

            injectField(typeNode, fieldDecl);
        }

        return true;
    }

    /* */
    private String getLogName(AnnotationValues<Logger> annotation)
    {
        String name = annotation.getInstance().name();

        if (name == null || !name.matches("[a-zA-Z0-9$]*"))
            return null;

        return name;
    }
}