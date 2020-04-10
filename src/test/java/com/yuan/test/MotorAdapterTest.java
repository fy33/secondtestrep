package com.yuan.test;

public class MotorAdapterTest {
    public static void main(String[] args) {
        System.out.println("适配器模式测试");

    }
}
interface Motor{
    void drive();
}
class ElectricMotor{
    public void electricDrive()
    {
        System.out.println("电能发动汽车");
    }

}
class OpticalMotor{
    public void opticalDrive()
    {
        System.out.println("光能发动机驱动汽车");
    }
}
class ElectricAdapter implements Motor{
    private ElectricMotor motor;
    public ElectricAdapter()
    {
        motor=new ElectricMotor();
    }
    public void drive()
    {
        motor.electricDrive();
    }
}
class OpticalAdapter implements Motor{
    private OpticalMotor motor;
    public OpticalAdapter()
    {
        motor=new OpticalMotor();
    }
    public void drive()
    {
        motor.opticalDrive();
    }
}