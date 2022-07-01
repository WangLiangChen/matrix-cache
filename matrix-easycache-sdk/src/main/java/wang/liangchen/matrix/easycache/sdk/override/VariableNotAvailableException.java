package wang.liangchen.matrix.easycache.sdk.override;

import org.springframework.expression.EvaluationException;

/**
 * A specific {@link EvaluationException} to mention that a given variable
 * used in the expression is not available in the context.
 *
 * @author Stephane Nicoll
 * @since 4.0.6
 */
@SuppressWarnings("serial")
class VariableNotAvailableException extends EvaluationException {

    private final String name;


    public VariableNotAvailableException(String name) {
        super("Variable not available");
        this.name = name;
    }


    public final String getName() {
        return this.name;
    }

}
