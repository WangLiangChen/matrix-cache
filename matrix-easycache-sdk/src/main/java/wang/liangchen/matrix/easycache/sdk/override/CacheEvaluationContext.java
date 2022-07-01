package wang.liangchen.matrix.easycache.sdk.override;


import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Cache specific evaluation context that adds a method parameters as SpEL
 * variables, in a lazy manner. The lazy nature eliminates unneeded
 * parsing of classes byte code for parameter discovery.
 *
 * <p>Also define a set of "unavailable variables" (i.e. variables that should
 * lead to an exception right the way when they are accessed). This can be useful
 * to verify a condition does not match even when not all potential variables
 * are present.
 *
 * <p>To limit the creation of objects, an ugly constructor is used
 * (rather then a dedicated 'closure'-like class for deferred execution).
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

    private final Set<String> unavailableVariables = new HashSet<>(1);


    CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
                           ParameterNameDiscoverer parameterNameDiscoverer) {

        super(rootObject, method, arguments, parameterNameDiscoverer);
    }


    /**
     * Add the specified variable name as unavailable for that context.
     * Any expression trying to access this variable should lead to an exception.
     * <p>This permits the validation of expressions that could potentially a
     * variable even when such variable isn't available yet. Any expression
     * trying to use that variable should therefore fail to evaluate.
     */
    public void addUnavailableVariable(String name) {
        this.unavailableVariables.add(name);
    }


    /**
     * Load the param information only when needed.
     */
    @Override
    @Nullable
    public Object lookupVariable(String name) {
        if (this.unavailableVariables.contains(name)) {
            throw new VariableNotAvailableException(name);
        }
        return super.lookupVariable(name);
    }

}
