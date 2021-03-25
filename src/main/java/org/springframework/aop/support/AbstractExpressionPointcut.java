package org.springframework.aop.support;

import java.io.Serializable;

public class AbstractExpressionPointcut implements ExpressionPointcut,Serializable {

    private String location;

    private String expression;

    public void setLocation(String location)
    {
        this.location=location;
    }

    private String getLocation(){return this.location;}

    public void setExpression(String expression)
    {
        this.expression=expression;
        try{
            onSetExpression(expression);
        }
        catch (IllegalArgumentException ex)
        {
            if(this.location!=null)
            {
                throw new IllegalStateException("Invalid expression at location["+this.location+"]:"+ex);
            }else{
                throw ex;
            }
        }
    }

    protected void onSetExpression(String expression)throws IllegalArgumentException{

    }

    @Override
    public String getExpression(){
        return this.expression;
    }

}
