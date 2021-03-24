package org.springframework.aop.aspectj;

import com.sun.istack.Nullable;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.PointcutParameter;
import org.springframework.aop.AopInvocationException;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.MethodMatchers;
import org.springframework.aop.support.StaticMethodMatcher;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAspectJAdvice implements Advice,AspectJPrecedenceInformation,Serializable {

    protected static final String JOIN_POINT_KEY=JoinPoint.class.getName();


    public static JoinPoint currentJoinPoint()
    {
        MethodInvocation mi= ExposeInvocationInterceptor.currentInvocation();
        if(!(mi instanceof ProxyMethodInvocation))
        {
            throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation:"+mi);
        }
        ProxyMethodInvocation pmi=(ProxyMethodInvocation)mi;
        JoinPoint jp=(JoinPoint)pmi.getUserAttribute(JOIN_POINT_KEY);
        if(jp==null)
        {
            jp=new MethodInvocationProceedingJoinPoint(pmi);
            pmi.setUserAttribute(JOIN_POINT_KEY,jp);
        }
        return jp;

    }

    private  final Class<?> declaringClass;

    private final String methodName;

    private final Class<?> parameterTypes;

    protected  transient Method aspectJAdviceMethod;

    private final AspectJExpressionPointcut pointcut;

    private final AspectInstanceFactory aspectInstanceFactory;

    private int declarationOrder;

    @Nullable
    private String[] argumentNames;

    @Nullable
    private String throwingName;

    @Nullable
    private String returningName;

    private Class<?> discoverReturningType=Object.class;

    private Class<?> discoveredThrowingType=Object.class;

    private int joinPointArgumentIndex=-1;

    private int joinPointStaticPartArgumentIndex=-1;

    @Nullable
    private Map<String,Integer> argumentBindings;

    private boolean argumentsIntrospected =false;

    @Nullable
    private Type discoveredReturningGenericType;

    public AbstractAspectJAdvice(
            Method aspectJAdviceMethod,AspectJExpressionPointcut pointcut,AspectInstanceFactory aspectInstanceFactory
    )
    {
        Assert.notNull(aspectJAdviceMethod,"Advice method must not be null");
        this.declaringClass=aspectJAdviceMethod.getDeclaringClass();
        this.methodName;=aspectJAdviceMethod.getName();
        this.parameterTypes=aspectJAdviceMethod.getParameterTypes();
        this.aspectJAdviceMethod=aspectJAdviceMethod;
        this.pointcut=pointcut;
        this.aspectInstanceFactory=aspectInstanceFactory;
    }

    public final Method getAspectJAdviceMethod()
    {
        return this.aspectJAdviceMethod;
    }

    public final AspectJExpressionPointcut getPointcut()
    {
        calculateArgumentBindings();
        return this.pointcut;
    }

    public final Pointcut buildSafePointcut()
    {
        Pointcut pc=getPointcut();
        MethodMatcher safeMethodMatcher= MethodMatchers.intersection(
                new AbstractAspectJAdvice.AdviceExcludingMethodMatcher(this.aspectJAdviceMethod),pc.getMethodMatcher()
        );
        return new ComposablePointcut(pc.getClassFilter(),safeMethodMatcher);
    }

    public final AspectInstanceFactory getAspectInstanceFactory()
    {
        return this.aspectInstanceFactory;
    }

    @Nullable
    public final ClassLoader getAspectClassLoader()
    {
        return this.aspectInstanceFactory.getAspectClassLoader();
    }

    @Override
    public int getOrder()
    {
        return this.aspectInstanceFactory.getOrder();
    }

    public void serAspectName(String name)
    {
        return this.aspectName;
    }

    public void setDeclarationOrder(int Order)
    {
        this.declarationOrder=order;
    }

    @Override
    public int getDeclarationOrder()
    {
        return this.declarationOrder;
    }

    public void setArgumentNamesForStringArray(String... args) {
        this.argumentNames = new String[args.length];
        for (int i = 0; i < args.length; i++)
        {
            this.argumentNames[i]= StringUtils.trimWhitespace(args[i]);
            if(!isVariableName(this.argumentNames[i]))
            {
                throw new IllegalArgumentException(
                        "'argumentNames' property of AbstractAspectJAdvice contains an argument name '"+
                                this.argumentNames[i]+"'that is not a valid Java identifier");


            }
        }
        if(this.argumentNames!=null)
        {
            if(this.aspectJAdviceMethod.getParameterCount()==this.argumentNames.length+1)
            {
                Class<?> firstArgType=this.aspectJAdviceMethod.getParameterTypes()[0];
                if(firstArgType==JoinPoint.class||
                        firstArgType==ProceedingJoinPoint.class||
                        firstArgType==JoinPoint.StaticPart.class)
                {
                    String[] oldNames=this.argumentNames;
                    this.argumentNames=new String[oldNames.length+1];
                    this.argumentNames[0]="THIS_JOIN_POINT";
                    System.arraycopy(oldNames,0;this.argumentNames,1,oldNames.length);
                }
            }
        }
    }

    public void setReturningName(String name)
    {
        throw new UnsupportedOperationException("Only afterReturning advice can be used to bind a return value");
    }

    protected void setReturningNameNoCheck(String name)
    {
        if(isVariableName(name))
        {
            this.returningName=name;
        }
        else{
            try{
                this.discoveredReturningGenericType= ClassUtils.forName(name,getAspectClassLoader());
            }
            catch(Throwable ex)
            {
                throw new IllegalArgumentException("Returning name '"+name+
                "' is neither a valid argument name nor the fully-qualified"+
                "name of a java type on the classpath. Root cause:"+ex);

            }
        }
    }

    protected  Class<?> getDiscoveredReturningType()
    {
        return this.discoverReturningType;
    }

    @Nullable
    protected Type getDiscoveredReturningGenericType()
    {
        return this.discoveredReturningGenericType;
    }

    public void setThrowingName(String name)
    {
        throw new UnsupportedOperationException("Only afterThrowing advice can be used to bind a thrown exception");
    }

    protected void setThrowingNameNoCheck(String name)
    {
        if(isVariableName(name))
        {
            this.throwingName=name;
        }
        else{
            try{
                this.discoveredThrowingType=ClassUtils.forName(name,getAspectClassLoader());
            }
            catch(Throwable ex)
            {
                throw new IllegalArgumentException("Throwing name '"+name+
                "'is neither a valid argument name nor the fully-qualified "+
                "name of a Java type on the classPath. Root cause: "+ex);
            }
        }
    }

    protected Class<?> getDiscoveredThrowingType()
    {
        return this.discoveredThrowingType;
    }

    private boolean isVariableName(String name)
    {
        char[] chars=name.toCharArray();
        if(!Character.isJavaIdentifierStart(chars[0]))
        {
            return false;
        }
        for(int i=1;i<chars.length;i++)
        {
            if(!Character.isJavaIdentifierPart(chars[i]))
            {
                return false;
            }
        }
        return true;
    }

    public final synchronized  void calculateArgumentBindings() {
        if (this.argumentsIntrospected || this.parameterTypes.length == 0) {
            return;
        }
        int numUnboundArgs = this.parameterTypes.length;
        Class<?>[] parameterTypes = this.aspectJAdviceMethod.getParameterTypes();
        if (maybeBindJoinPoint(parameterTypes[0] || maybeBindProceedingJointPoint(parameterTypes[0]) ||
                maybeBindJoinPointStaticPart(parameterTypes[0]))) {

            numUnboundArgs--;
        }
        if (numUnboundArgs > 0)
        {
            bindArgumentsByName(numUnboundArgs);
        }
        this.argumentsIntrospected=true;
    }

    private boolean maybeBindJoinPoint(Class<?> candidateParameterType)
    {
        if(JoinPoint.class==candidateParameterType)
        {
            this.joinPointArgumentIndex=0;
            return true;
        }
        else {
            return false;
        }
    }

    private boolean maybeBindProceedingJointPoint(Class<?> candidateParameterType)
    {
        if(ProceedingJoinPoint.class==candidateParameterType)
        {
            if(!supportsProceedingJoinPoint())
            {
                throw new IllegalArgumentException("ProceedingJoinPoint is only supported for around advice");

            }
            this.joinPointArgumentIndex=0;
            return true;
        }
        else {
            return false;
        }
    }

    protected boolean supportsProceedingJoinPoint()
    {
        return false;
    }

    private boolean maybeBindJoinPointStaticPart(Class<?> candidateParameterType)
    {
        if(JoinPoint.StaticPart.class==candidateParameterType) {
            this.joinPointStaticPartArgumentIndex = 0;
            return true;
        }
        else {
            return false;
        }
    }

    private void bindArgumentsByName(int numArgumentsExpectingToBind)
    {
        if(this.argumentNames==null)
        {
            this.argumentNames=createParameterNameDiscoverer().getParameterNames(this.aspectJAdviceMethod);

        }
        if(this.argumentNames!=null)
        {
            bindExplicitArguments(numArgumentsExpectingToBind);
        }
        else {
            throw new IllegalStateException("Advice method["+this.aspectJAdviceMethod.getName()+"]"+
            "requires "+numArgumentsExpectingToBind+" arguments to be bound by name,but "+
            "the argument names were not specified and could not be discovered.");
        }
    }

    protected ParameterNameDiscoverer createParameterNameDiscoverer()
    {
        DefaultParameterNameDiscoverer discoverer=new DefaultParameterNameDiscoverer();
        AspectJAdviceParameterNameDiscoverer adviceParameterNameDiscoverer=
                new AspectJAdviceParameterNameDiscoverer(this.pointcut.getExpression());
        adviceParameterNameDiscoverer.setReturningName(this.returningName);
        adviceParameterNameDiscoverer.setThrowingName(this.throwingName);
        adviceParameterNameDiscoverer.setRaiseExceptions(true);
        discoverer.addDiscoverer(adviceParameterNameDiscoverer);
        return discoverer;
    }

    private void bindExplicitArguments(int numArgumentLeftToBind)
    {
        Assert.state(this.argumentNames!=null,"No argument names available");
        this.argumentBindings=new HashMap<>();
        int numExpectedArgumentNames=this.aspectJAdviceMethod.getParameterCount();
        if(this.argumentNames.length!=numExpectedArgumentNames)
        {
            throw new IllegalStateException("Exception to find "+numExpectedArgumentNames+
            "arguments to bind by name in advice,but actually found "+
            this.argumentNames.length+" arguments.");
        }

        int argumentIndexOffset=this.parameterTypes.length-numArgumentLeftToBind;
        for(int i=argumentIndexOffset;i<this.argumentNames.length;i++)
        {
            this.argumentBindings.put(this.argumentnames[i],i);
        }
        if(this.returningName!=null)
        {
            if(!this.argumentBindings.containsKey(this.returningName))
            {
                throw new IllegalStateException("Returning argument name '"+this.returningName+
                "'was not bound in advice arguments");
            }
            else {
                Integer index=this.argumentBindings.get(this.returningName);
                this.discoveredReturningType=this.aspectJAdviceMethod.getParameterTypes()[index];
                this.discoveredReturningGenericType=this.aspectJAdviceMethod.getGenericParameterTypes()[index];
            }
        }
        if(this.throwingName!=null)
        {
            if(!this.argumentBindings.containsKey(this.throwingName))
            {
                throw new IllegalStateException("Throwing argument name '"+this.throwingName+
                "'was not bound in advice arguments");

            }
            else{
                Integer index=this.argumentBindings.get(this.throwingName);
                this.discoveredReturningType=this.aspectJAdviceMethod.getParameterTypes();
                this.discoveredReturningGenericType=this.aspectJAdviceMethod.getGenericParameterTypes()[index];

            }
        }
        if(this.throwingName!=null)
        {
            if(!this.argumentBindings.containsKey(this.throwingName))
            {
                throw new IllegalStateException("Throwing argument name '"+this.throwingName+
                "'was not bound in advice argumentss");
            }
            else{
                Integer index=this.argumentBindings.get(this.throwingName);
                this.discoveredThrowingType=this.aspectJAdviceMethod.getParameterTypes()[index];
            }
        }
        configurePointcutParameters(this.argumentNames,argumentIndexOffset);

    }

    private void configurePointcutParameters(String[] argumentNames,int argumentIndexOffset)
    {
        int numParametersToRemove=argumentIndexOffset;
        if(this.returningName!=null)
        {
            numParametersToRemove++;
        }
        if(this.throwingName!=null)
        {
            numParametersToRemove++;
        }
        String[] pointcutParameterNames=new String[argumentNames.length-numParametersToRemove];
        int index=0;
        for(int i=0;i<argumentNames.length;i++)
        {
            if(i<argumentIndexOffset)
            {
                continue;
            }
            if(argumentNames[i].equals(this.returningName)||
                    argumentNames[i].equals(this.throwingName))
            {
                continue;
            }
            pointcutParameterNames[index]=argumentNames[i];
            pointcutParameterTypes[index]=methodParameterTypes[i];
            index++;
        }
        this.pointcut.setParameterNames(pointcutParameterNames);
        this.pointcut.setParameterTypes(pointcutParameterTypes);

    }

    protected Object[] argBinding(JoinPoint jp, @Nullable JoinPointMatch jpMatch,
                                  @Nullable Object returnValue,@Nullable Throwable ex)
    {
        calculateArgumentBindings();
        Object[] adviceInvocationArgs=new Object[this.parameterTypes.length];
        int numBound=0;
        if(this.joinPointArgumentIndex!=-1)
        {
            adviceInvocationArgs[this.joinPointArgumentIndex]=jp;
            numBound++;
        }
        else if(this.joinPointStaticPartArgumentIndex!=-1)
        {
            adviceInvocationArgs[this.joinPointStaticPartArgumentIndex]=jp.getStaticPart();
            numBound++;
        }
        if(!CollectionUtils.isEmpty(this.argumentBindings)) {
            if (jpMatch != null) {
                PointcutParameter[] parameterBindings = jpMatch.getParameterBindings();
                for (PointcutParameter parameter : parameterBindings) {
                    String name = parameter.getName();
                    Integer index = this.argumentBindings.get(name);
                    adviceInvocationArgs[index] = parameter.getBinding();
                    numBound++;
                }
            }

            //binding from returning clause
            if (this.returningName != null) {
                Integer index = this.argumentBindings.get(this.returningName);
                adviceInvocationArgs[index] = returnValue;
                numBound++;
            }
            if (this.throwingName != null)
            {
                Integer index=this.argumentBindings.get(this.throwingName);
                adviceInvocationArgs[index]=ex;
                numBound++;
            }
        }
        if(numBound!=this.parameterTypes.length)
        {
            throw new IllegalStateException("Required to bind "+this.parameterTypes.length+
            "arguments ,but noly bound "+numBound+"(JoinPointMatch"+
                    (jpMatch==null?"was NOT":"WAS")+" bound ininvocation");
        }
        return adviceInvocationArgs;

    }

    protected Object invokeAdviceMethod(
            @Nullable JoinPointMatch jpMatch,@Nullable Object returnValue,@Nullable Throwable ex
    )throws Throwable{
        return invokeAdviceMethodWithGivenArgs(argumentBindings(getJoinPoint(),jpMatch,returnValue,ex))
    }

    protected  Object invokeAdviceMethod(JoinPoint jp,@Nullable JoinPointMatch jpMatch,
                                         @Nullable Object returnValue,@Nullable Throwable t)throws Throwable
    {
        return invokeAdviceMethodWithGivenArgs(argBinding(getJoinPoint,jpMatch,returnValue,ex));
    }

    protected  Object invokeAdviceMethod(JointPoint jp,@Nullable JoinPointMatch jpMatch,
                                         @Nullable Object returnValue,@Nullable Throwable t)throws Throwable

    {
        return invokeAdviceMethodWithGivenArgs(argBinding(jp,jpMatch,returnValue,t));
    }

    protected Object invokeAdviceMethodWithGivenArgs(Object[] args)throws Throwable{
        Object[] actualArgs=args;
        if(this.aspectJAdviceMethod.getParameterCount()==0)
        {
            actualArgs=null;
        }
        try{
            reflectionUtils.makeAccessible(this.aspectJAdviceMethod);
            return this.aspectJAdviceMethod.invoke(this.aspectInstanceFactory.getAspectInstance())

        }
        catch(IllegalArgumentException ex)
        {
            throw new AopInvocationException("Mismatch on arguments to advice method ["+
            this.aspectJAdviceMethod+"];pointcut expression["+
            this.pointcut.getPointcutExpression()+"]",ex);
        }
        catch(InvocationTargetException ex)
        {
            throw ex.getTargetException();
        }
    }

    protected JoinPoint getJoinPoint()
    {
        return currentJoinPoint();
    }

    @Nullable
    protected JoinPointMatch getJoinPointMatch()
    {
        MethodInvocation mi=ExposeInvocationInterceptor.currentInvocation();
        if(!(mi instanceof ProxyMethodInvocation))
        {
            throw new IllegalStateException("Method Invocation is not a Spring ProxyMethodInvocation:"+mi);

        }
        return getJoinPointMatch((ProxyMethodInvocation)mi);
    }

    @Nullable
    protected JoinPointMatch getJoinPointMatch(ProxyMethodInvocation pmi)
    {
        String expression =this.pointcut.getExpression();
        return (expression!=null?(JoinPointMatch)pmi.getUserAttribute(expression):null);
    }

    @Override
    public String toString()
    {
        return getClass().getName()+" :advice method ["+this.aspectJAdviceMethod+"];"+
                "aspect name '"+this.aspectName +"'";
    }

    private void readObject(ObjectInputStream inputStream) throws IOException,ClassNotFoundException{
        inputStream.defaultReadObject();
        try{
            this.aspectJAdviceMethod =this.declaringClass.getMethod(this.methodName,this.parameterTypes);

        }catch (NoSuchMethodException ex)
        {
            throw new IllegalStateException("Failed to find advice method on deserialization ",ex);

        }
    }

    private static class AdviceExcludingMethodMatcher extends StaticMethodMatcher{
        private final Method adviceMethod;
        public AdviceExcludingMethodMatcher(Method adviceMethod)
        {
            this.adviceMethod=adviceMethod;
        }

        @Override
        public boolean equals(Object other)
        {
            if(this==other)
            {
                return true;
            }
            if(!(other instanceof AdviceExcludingMethodMatcher))
            {
                return false;
            }
            AdviceExcludingMethodMatcher otherMm=(AdviceExcludingMethodMatcher)other;
            return this.adviceMethod.equals(otherMm.adviceMethod);
        }

        @Override
        public int hashCode()
        {
            return this .adviceMethod.hashCode();
        }
    }
}
