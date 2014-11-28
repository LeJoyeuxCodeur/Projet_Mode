package data;

import java.awt.Image;
import java.awt.Toolkit;

import objects.Vecteur;

public class Constantes {
	public static final int COEFF1 = 500, COEFF2 = 300;
	public static final Vecteur lumiere = new Vecteur(0, 0, 1);
	public static final Image wallpaper = Toolkit.getDefaultToolkit().getImage("ressources/degrade.jpg");
	public static final String FIGURE_BASE = "ressources/gts/tie.gts";
	public static double alphaX=0.0, alphaY=0.0;
}