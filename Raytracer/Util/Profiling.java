package Raytracer.Util;

import Raytracer.SDRaytracer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Profiling {
    public static Object profilingDecorator(Class c) {
        long start = System.currentTimeMillis();
        try {
            Object instance = c.newInstance();

            long end = System.currentTimeMillis();
            long time = end - start;
            System.out.println("time: " + time + " ms");

            if (instance instanceof SDRaytracer){
                SDRaytracer i = (SDRaytracer)instance;
                System.out.println("nrprocs=" + i.nrOfProcessors);
                return i;
            }

        } catch (InstantiationException | IllegalAccessException ex) {
            Logger.getLogger(Profiling.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void profileRenderImage(SDRaytracer s) {
        long end;
        long start;
        long time;

        s.renderImage(); // initialisiere Datenstrukturen, erster Lauf verf�lscht sonst Messungen

        for (int procs = 1; procs < 6; procs++) {

            s.maxRec = procs - 1;
            System.out.print(procs);
            for (int i = 0; i < 10; i++) {
                start = System.currentTimeMillis();

                s.renderImage();

                end = System.currentTimeMillis();
                time = end - start;
                System.out.print(";" + time);
            }
            System.out.println("");
        }
    }

}
