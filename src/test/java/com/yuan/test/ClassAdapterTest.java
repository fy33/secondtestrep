package com.yuan.test;

public class ClassAdapterTest {
    public static void main(String[] args) {
        System.out.println("类适配器模式测试");
        Target target=new ClassAdapter();
        target.request();
    }
}
interface  Target{
     void request();
}
class Adapter{
    public void specificReqeust()
    {
        System.out.println("适配者中的业务代码被调用");
    }
}
class ClassAdapter extends Adapter implements Target{

    @Override
    public void request() {
        specificReqeust();
    }
}