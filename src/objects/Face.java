package objects;

import exceptions.MalFormedFaceException;

public class Face implements Comparable<Face>{
	private Point a, b, c;
	private Vecteur normal;
	private int barycentre;

	public Face(Segment a, Segment b, Segment c) {
		this.a = a.getOrigine();
		this.b = a.getFin();
		
		if(!b.getOrigine().equals(this.a) && !b.getOrigine().equals(this.b))
			this.c = b.getOrigine();
		else if(!b.getFin().equals(this.a) && !b.getFin().equals(this.b))
			this.c = b.getFin();
		else
			throw new MalFormedFaceException();
		normalisation();
		barycentre();
	}
	
	public void barycentre(){
		barycentre=(int)(a.getZ()+b.getZ()+c.getZ())/3;
	}
	
	public Vecteur getNormal(){
		return this.normal;
	}

	public String toString() {
		return a + ";" + b + ";" + c;
	}

	public Point getSommetA() {
		return a;
	}

	public Point getSommetB() {
		return b;
	}

	public Point getSommetC() {
		return c;
	}
	public boolean equals(Face f){
		return a.equals(f.a) && b.equals(f.b) && c.equals(f.c);
	}
	public void normalisation(){
		this.normal = new Vecteur(new Vecteur(this.a, this.b), new Vecteur(this.a, this.c));
	}

	@Override
	public int compareTo(Face arg0) {
		// TODO Auto-generated method stub
		return this.barycentre-arg0.barycentre;
	}
}
