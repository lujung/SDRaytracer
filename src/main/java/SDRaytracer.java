package src.main.java;

import src.main.java.Datatypes.*;
import src.main.java.Profiling.Profiling;
import src.main.java.Scenes.Scenes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static src.main.java.Profiling.Profiling.profilingDecorator;

/* Implementation of a very simple src
   Stephan Diehl, Universit�t Trier, 2010-2016
*/


public class SDRaytracer extends JFrame {
    private static final long serialVersionUID = 1L;
    int width = 1000;
    int height = 1000;

    private Future[] futureList = new Future[width];
    public int nrOfProcessors = Runtime.getRuntime().availableProcessors();
    private ExecutorService eservice = Executors.newFixedThreadPool(nrOfProcessors);

    public int maxRec = 3;
    int rayPerPixel = 1;
    int startX, startY, startZ;

    private List<Triangle> triangles;

    RGB[][] image = new RGB[width][height];

    private float fovX = (float) 0.628;
    private float fovY = (float) 0.628;
    private RGB ambientColor = new RGB(0.01f, 0.01f, 0.01f);
    RGB backgroundColor = new RGB(0.05f, 0.05f, 0.05f);
    private RGB black = new RGB(0.0f, 0.0f, 0.0f);
    private int yAngleFactor = 4, xAngleFactor = -4;

    public static void main(String argv[]) {
        SDRaytracer s = (SDRaytracer) profilingDecorator(SDRaytracer.class);
    }

    public SDRaytracer() {
        boolean profiling = false;
        createScene();

        if (!profiling) renderImage();
        else Profiling.profileRenderImage(this);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = this.getContentPane();
        contentPane.setLayout(new BorderLayout());
        JPanel area = new JPanel() {
            public void paint(Graphics g) {
                System.out.println("fovX=" + fovX + ", fovY=" + fovY + ", xangle=" + xAngleFactor + ", yangle=" + yAngleFactor);
                if (image == null) return;
                for (int i = 0; i < width; i++)
                    for (int j = 0; j < height; j++) {
                        g.setColor(image[i][j].color());
                        // zeichne einzelnen Pixel
                        g.drawLine(i, height - j, i, height - j);
                    }
            }
        };

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                boolean redraw = false;
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    xAngleFactor--;
                    //mainLight.position.y-=10;
                    //fovX=fovX+0.1f;
                    //fovY=fovX;
                    //maxRec--; if (maxRec<0) maxRec=0;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    xAngleFactor++;
                    //mainLight.position.y+=10;
                    //fovX=fovX-0.1f;
                    //fovY=fovX;
                    //maxRec++;if (maxRec>10) return;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    yAngleFactor--;
                    //mainLight.position.x-=10;
                    //startX-=10;
                    //fovX=fovX+0.1f;
                    //fovY=fovX;
                    redraw = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    yAngleFactor++;
                    //mainLight.position.x+=10;
                    //startX+=10;
                    //fovX=fovX-0.1f;
                    //fovY=fovX;
                    redraw = true;
                }
                if (redraw) {
                    createScene();
                    renderImage();
                    repaint();
                }
            }
        });

        area.setPreferredSize(new Dimension(width, height));
        contentPane.add(area);
        this.pack();
        this.setVisible(true);
    }

    Ray eye_ray = new Ray();
    double tanFovX;
    double tanFovY;

    public void renderImage() {
        tanFovX = Math.tan(fovX);
        tanFovY = Math.tan(fovY);
        for (int i = 0; i < width; i++) {
            futureList[i] = (Future) eservice.submit(new RaytraceTask(this, i));
        }

        for (int i = 0; i < width; i++) {
            try {
                RGB[] col = (RGB[]) futureList[i].get();
                for (int j = 0; j < height; j++)
                    image[i][j] = col[j];
            } catch (InterruptedException | ExecutionException e) {
            }
        }
    }


    RGB rayTrace(Ray ray, int rec) {
        if (rec > maxRec) return black;
        IPoint ip = hitObject(ray);  // (ray, p, n, triangle);
        if (ip.dist > IPoint.epsilon)
            return lighting(ray, ip, rec);
        else
            return black;
    }


    private IPoint hitObject(Ray ray) {
        IPoint isect = new IPoint(null, null, -1);
        float idist = -1;
        for (Triangle t : triangles) {
            IPoint ip = ray.intersect(t);
            if (ip.dist != -1)
                if ((idist == -1) || (ip.dist < idist)) { // save that intersection
                    idist = ip.dist;
                    isect.ipoint = ip.ipoint;
                    isect.dist = ip.dist;
                    isect.triangle = t;
                }
        }
        return isect;  // return intersection point and normal
    }


    RGB addColors(RGB c1, RGB c2, float ratio) {
        return new RGB((c1.red + c2.red * ratio),
                (c1.green + c2.green * ratio),
                (c1.blue + c2.blue * ratio));
    }

    private RGB lighting(Ray ray, IPoint ip, int rec) {
        Vec3D point = ip.ipoint;
        Triangle triangle = ip.triangle;
        RGB color = addColors(triangle.color, ambientColor, 1);
        Ray shadow_ray = new Ray();
        for (Light light : Scenes.lights) {
            shadow_ray.start = point;
            shadow_ray.dir = light.position.minus(point).mult(-1);
            shadow_ray.dir.normalize();
            IPoint ip2 = hitObject(shadow_ray);
            if (ip2.dist < IPoint.epsilon) {
                float ratio = Math.max(0, shadow_ray.dir.dot(triangle.normal));
                color = addColors(color, light.color, ratio);
            }
        }
        Ray reflection = new Ray();
        //R = 2N(N*L)-L)    L ausgehender Vektor
        Vec3D L = ray.dir.mult(-1);
        reflection.start = point;
        reflection.dir = triangle.normal.mult(2 * triangle.normal.dot(L)).minus(L);
        reflection.dir.normalize();
        RGB rcolor = rayTrace(reflection, rec + 1);
        float ratio = (float) Math.pow(Math.max(0, reflection.dir.dot(L)), triangle.shininess);
        color = addColors(color, rcolor, ratio);
        return (color);
    }

    private void createScene() {
        triangles = new ArrayList<Triangle>();
        Scenes.getExampleScene(triangles, yAngleFactor, xAngleFactor);
    }

    static class RaytraceTask implements Callable {
        private SDRaytracer tracer;
        private int i;

        RaytraceTask(SDRaytracer t, int ii) {
            tracer = t;
            i = ii;
        }

        public RGB[] call() {
            RGB[] col = new RGB[tracer.height];
            for (int j = 0; j < tracer.height; j++) {
                tracer.image[i][j] = new RGB(0, 0, 0);
                for (int k = 0; k < tracer.rayPerPixel; k++) {
                    double di = i + (Math.random() / 2 - 0.25);
                    double dj = j + (Math.random() / 2 - 0.25);
                    if (tracer.rayPerPixel == 1) {
                        di = i;
                        dj = j;
                    }
                    Ray eye_ray = new Ray();
                    eye_ray.setStart(tracer.startX, tracer.startY, tracer.startZ);   // ro
                    eye_ray.setDir((float) (((0.5 + di) * tracer.tanFovX * 2.0) / tracer.width - tracer.tanFovX),
                            (float) (((0.5 + dj) * tracer.tanFovY * 2.0) / tracer.height - tracer.tanFovY),
                            (float) 1f);    // rd
                    eye_ray.normalize();
                    col[j] = tracer.addColors(tracer.image[i][j], tracer.rayTrace(eye_ray, 0), 1.0f / tracer.rayPerPixel);
                }
            }
            return col;
        }
    }
}

