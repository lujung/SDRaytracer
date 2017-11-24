package Raytracer.Scenes;

import Raytracer.Datatypes.*;

import java.util.List;

public class Scenes {
    private static Light mainLight = new Light(new Vec3D(0, 100, 0), new RGB(0.1f, 0.1f, 0.1f));
    public static Light[] lights = new Light[]{mainLight
            , new Light(new Vec3D(100, 200, 300), new RGB(0.5f, 0, 0.0f))
            , new Light(new Vec3D(-100, 200, 300), new RGB(0.0f, 0, 0.5f))
            //,new Raytracer.Datatypes.Light(new Raytracer.Datatypes.Vec3D(-100,0,0), new Raytracer.Datatypes.RGB(0.0f,0.8f,0.0f))
    };

    public static void getExampleScene(List<Triangle> triangles, int y_angle_factor, int x_angle_factor) {
        addCube(triangles, 0, 35, 0, 10, 10, 10, new RGB(0.3f, 0, 0), 0.4f);       //rot, klein
        addCube(triangles, -70, -20, -20, 20, 100, 100, new RGB(0f, 0, 0.3f), .4f);
        addCube(triangles, -30, 30, 40, 20, 20, 20, new RGB(0, 0.4f, 0), 0.2f);        // gr�n, klein
        addCube(triangles, 50, -20, -40, 10, 80, 100, new RGB(.5f, .5f, .5f), 0.2f);
        addCube(triangles, -70, -26, -40, 130, 3, 40, new RGB(.5f, .5f, .5f), 0.2f);

        Matrix mRx = Matrix.createXRotation((float) (x_angle_factor * Math.PI / 16));
        Matrix mRy = Matrix.createYRotation((float) (y_angle_factor * Math.PI / 16));
        Matrix mT = Matrix.createTranslation(0, 0, 200);
        Matrix m = mT.mult(mRx).mult(mRy);
        m.print();
        m.apply(triangles);
    }

    private static void addCube(List<Triangle> triangles, int x, int y, int z, int w, int h, int d, RGB c, float sh) {  //front
        triangles.add(new Triangle(new Vec3D(x, y, z), new Vec3D(x + w, y, z), new Vec3D(x, y + h, z), c, sh));
        triangles.add(new Triangle(new Vec3D(x + w, y, z), new Vec3D(x + w, y + h, z), new Vec3D(x, y + h, z), c, sh));
        //left
        triangles.add(new Triangle(new Vec3D(x, y, z + d), new Vec3D(x, y, z), new Vec3D(x, y + h, z), c, sh));
        triangles.add(new Triangle(new Vec3D(x, y + h, z), new Vec3D(x, y + h, z + d), new Vec3D(x, y, z + d), c, sh));
        //right
        triangles.add(new Triangle(new Vec3D(x + w, y, z), new Vec3D(x + w, y, z + d), new Vec3D(x + w, y + h, z), c, sh));
        triangles.add(new Triangle(new Vec3D(x + w, y + h, z), new Vec3D(x + w, y, z + d), new Vec3D(x + w, y + h, z + d), c, sh));
        //top
        triangles.add(new Triangle(new Vec3D(x + w, y + h, z), new Vec3D(x + w, y + h, z + d), new Vec3D(x, y + h, z), c, sh));
        triangles.add(new Triangle(new Vec3D(x, y + h, z), new Vec3D(x + w, y + h, z + d), new Vec3D(x, y + h, z + d), c, sh));
        //bottom
        triangles.add(new Triangle(new Vec3D(x + w, y, z), new Vec3D(x, y, z), new Vec3D(x, y, z + d), c, sh));
        triangles.add(new Triangle(new Vec3D(x, y, z + d), new Vec3D(x + w, y, z + d), new Vec3D(x + w, y, z), c, sh));
        //back
        triangles.add(new Triangle(new Vec3D(x, y, z + d), new Vec3D(x, y + h, z + d), new Vec3D(x + w, y, z + d), c, sh));
        triangles.add(new Triangle(new Vec3D(x + w, y, z + d), new Vec3D(x, y + h, z + d), new Vec3D(x + w, y + h, z + d), c, sh));

    }
}
