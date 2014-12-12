package io;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import objects.Face;
import objects.Point;
import objects.Segment;
import transformations.Homothetie;
import transformations.Rotation;
import transformations.Translation;
import appearance.MyUI;
import data.Data;
import database.GestionBDD;
import database.Requests;

public class Fenetre extends JFrame implements KeyListener, MouseWheelListener, MouseMotionListener, MouseListener {
	private static final long serialVersionUID = 1L;
	private Point current;
	private Random rd = new Random();
	private int cptMouse = 0, decalX, decalY, tailleSegment = 0;
	private int red = rd.nextInt(256), green, blue;
	private GtsReader reader;
	private List<Face> listeFaces;
	private List<Segment> listeSegments;
	private List<Point> listePoints;
	private JPanel modeles, panelTextField, panelGauche;
	private JFrame chargement;
	private DefaultListModel<String> dl;
	private JList<String> listeModeles;
	private Color color;
	private Map<Face, Color> map;
	private boolean export = false, fullScreen = false;
	private boolean ctrlA = false, moving = false;
	private Requests r = new Requests();
	private String modele = null, filtreTexte = "";
	private JTextField filtre = new JTextField();
	private JScrollPane scrollPane;
	
	public Fenetre() {
		super(Data.TITLE);
		long debut = System.currentTimeMillis();
		
		initFrameChargement();
		new GestionBDD(r);
		initJMenuBar();
		initFrame();
		paramListeModeles();
		modifFrame();
		addListeners();
		new MyUI();
		validate();
		paintComponent(getGraphics());
		long fin = System.currentTimeMillis();
		if(fin - debut < 2000){ // Toujours au moins avoir un temps de chargement de demarrage d'au moins 2 secondes
			try {
				Thread.sleep(2000 - (fin - debut));
			} 
			catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		chargement.dispose();
		setVisible(true);
	}

	private void initFrameChargement() {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

		chargement = new JFrame();
		chargement.setLocation(dim.width/2 - chargement.getSize().width/2 - (Data.CHARGEMENT.getIconWidth()/2), dim.height/2 - chargement.getSize().height/2 - (Data.CHARGEMENT.getIconHeight()/2));
		chargement.getContentPane().add(new JLabel(Data.CHARGEMENT));
		chargement.setUndecorated(true);
		chargement.setIconImage(Data.ICON3D);
		chargement.pack();
		chargement.setVisible(true);
	}

	private void modifFrame() {
		scrollPane = new JScrollPane(listeModeles);
		
		modeles = new JPanel();
		modeles.setLayout(new BoxLayout(modeles, BoxLayout.Y_AXIS));
		modeles.setBackground(Color.white);
		modeles.add(scrollPane);

		panelTextField = new JPanel();
		filtre.setPreferredSize(new Dimension(listeModeles.getFixedCellWidth(), 30));
		filtre.setFocusable(false);
		panelTextField.add(filtre);
		panelTextField.setMaximumSize(new Dimension(listeModeles.getFixedCellWidth(), 50));

		panelGauche = new JPanel();
		panelGauche.setLayout(new BoxLayout(panelGauche, BoxLayout.Y_AXIS));
		panelGauche.add(panelTextField);
		panelGauche.add(modeles);
		
		getContentPane().add(panelGauche, BorderLayout.WEST);
	}

	private void paramListeModeles() {
		List<String> tmp = r.select("nom");
		initListeModeles(tmp);
	}
	
	private void paramListeModelesCustom() {
		List<String> tmp = r.selectLike(filtreTexte);
		initListeModeles(tmp);
		listeModeles.setModel(dl);
		modeles.remove(scrollPane);
		scrollPane = new JScrollPane(listeModeles);
		initListenerJList();
		modeles.add(scrollPane);
		modeles.validate();
		modeles.repaint();
	}

	private void initListeModeles(List<String> tmp) {
		dl = new DefaultListModel<String>();
		for(int i = 0; i < tmp.size(); i++)
			dl.add(i, tmp.get(i));
		
		listeModeles = new JList<String>(dl);
	    listeModeles.setBackground(new Color(228, 228, 255));
	    listeModeles.setSelectionForeground(Color.RED);
	    listeModeles.setSelectionBackground(Color.LIGHT_GRAY);
	    listeModeles.setFixedCellHeight(30);
	    listeModeles.setFixedCellWidth(200);
	    listeModeles.setFont(new Font("Serif", Font.BOLD, 15));
		listeModeles.setFocusable(false);
		listeModeles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	private void initFrame() {
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

		setSize(dim.width, dim.height);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setIconImage(Data.ICON3D);
	}

	private void addListeners() {
		filtre.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				try{
					if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A)
						ctrlA = true;
					if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
						if(ctrlA)
							filtreTexte = "";
						else
							filtreTexte = filtreTexte.substring(0, filtreTexte.length() - 1);
					}
					if(Character.isLetterOrDigit(e.getKeyChar()))
						filtreTexte = filtreTexte + Character.toLowerCase(e.getKeyChar());
					paramListeModelesCustom();
				} catch (Exception ex){}
			}
		});
		filtre.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				setFocusable(false);
				filtre.setFocusable(true);
			}
		});
		addMouseWheelListener(this);
		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
		
		setDropTarget(new DropTarget(this, new DropTargetAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void drop(DropTargetDropEvent e) {
				e.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				for (DataFlavor flavor : e.getCurrentDataFlavors()) {
					try {
						List<File> files = (List<File>) e.getTransferable().getTransferData(flavor);
						File f = files.get(0);
						if(f.getAbsolutePath().substring(f.getAbsolutePath().length() - 4, f.getAbsolutePath().length()).equalsIgnoreCase(".gts")){
							r.insert(f.getName().toLowerCase(), f.getAbsolutePath());
							modele = r.selectWhere("nom = '" + f.getName().toLowerCase() + "'");
							updateModel();
						}
					} 
					catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		}));
		initListenerJList();
	}

	private void initListenerJList() {
		listeModeles.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent lse) {
				modele = r.selectWhere("nom = '" + listeModeles.getSelectedValue().toString() + "'");
				updateModel();
			}
		});		
	}

	private void initJMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		List<JMenu> jMenus = new LinkedList<JMenu>();
		List<JMenuItem> jMenuItems = new LinkedList<JMenuItem>();
		
		jMenus.add(new JMenu("Fichier"));
		jMenus.add(new JMenu("Edition"));
		jMenus.add(new JMenu("Affichage"));
		jMenus.add(new JMenu("Outils"));
		jMenus.add(new JMenu("Aide"));
		
		jMenuItems.add(new JMenuItem("Ouvrir"));
		jMenuItems.add(new JMenuItem("Enregistrer sous"));
		jMenuItems.add(new JMenuItem("Exporter"));
		jMenuItems.add(new JMenuItem("Quitter"));
		jMenuItems.add(new JMenuItem("Saisir taille"));
		jMenuItems.add(new JCheckBoxMenuItem("Plein ecran"));
		jMenuItems.add(new JMenuItem("Modifier la couleur"));
		jMenuItems.add(new JMenuItem("Degrade de couleurs"));
		jMenuItems.add(new JMenuItem("Couleur aleatoire (uniforme)"));
		jMenuItems.add(new JMenuItem("Toutes couleurs aleatoires"));
		jMenuItems.add(new JMenuItem("Changer image de fond..."));
		jMenuItems.add(new JMenuItem("You spin my head right round, right round..."));
		jMenuItems.add(new JMenuItem("A propos"));
		
		jMenuItems.get(0).setIcon(Data.OPEN);
		jMenuItems.get(1).setIcon(Data.SAVE);
		jMenuItems.get(2).setIcon(Data.EXPORT);
		jMenuItems.get(3).setIcon(Data.QUIT);
		jMenuItems.get(12).setIcon(Data.HELP);
		
		// Fichier
		jMenus.get(0).add(jMenuItems.get(0));
		jMenus.get(0).add(jMenuItems.get(1));
		jMenus.get(0).add(jMenuItems.get(2));
		jMenus.get(0).add(jMenuItems.get(3));
		// Edition
		jMenus.get(1).add(jMenuItems.get(4));
		// Affichage
		jMenus.get(2).add(jMenuItems.get(5));
		// Outils
		jMenus.get(3).add(jMenuItems.get(6));
		jMenus.get(3).add(jMenuItems.get(7));
		jMenus.get(3).add(jMenuItems.get(8));
		jMenus.get(3).add(jMenuItems.get(9));
		jMenus.get(3).add(jMenuItems.get(10));
		jMenus.get(3).add(jMenuItems.get(11));
		// Aide
		jMenus.get(4).add(jMenuItems.get(12));
		
		for(JMenu j : jMenus)
			menuBar.add(j);
		setJMenuBar(menuBar);
		
		// Bouton ouvrir
		jMenuItems.get(0).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileopen = null;
				try {
					fileopen = new JFileChooser(new File( "." ).getCanonicalPath());
				} catch (IOException e) {}
				FileFilter filter = new FileNameExtensionFilter(".gts", "gts");
				fileopen.addChoosableFileFilter(filter);

				int ret = fileopen.showDialog(null, "Open file");
				File f = fileopen.getSelectedFile();
				if (ret == JFileChooser.APPROVE_OPTION && f.getAbsolutePath().substring(f.getAbsolutePath().length() - 4, f.getAbsolutePath().length()).equalsIgnoreCase(".gts")){
					r.insert(fileopen.getSelectedFile().getName().toLowerCase(), fileopen.getSelectedFile().getAbsolutePath());
					modele = r.selectWhere("nom = '" + fileopen.getSelectedFile().getName().toLowerCase() + "'");
					updateModel();
				}
			}
		});
		
		// boutton exporter
		jMenuItems.get(2).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				exportImage();
			}
		});
		
		// Bouton quitter
		jMenuItems.get(3).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose(); // Dans le cas ou plusieurs fenetres sont ouvertes, une seule est fermee
			}
		});
		
		// Bouton editer taille
		jMenuItems.get(4).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				editSize();
			}
		});
		
		// Bouton full Screen
		jMenuItems.get(5).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setFullScreen();
			}
		});
		
		
		// Bouton modif couleur
		jMenuItems.get(6).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				map = null;
				color = JColorChooser.showDialog(null, "Palette de couleur", null);
				paintComponent(getGraphics());
			}
		});
		
		// Bouton degrade couleur
		jMenuItems.get(7).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				map = new HashMap<Face, Color>();
				int etape = listeFaces.size() / 255;
				Color tmp = degradeColor();

				Collections.sort(listeFaces, new Comparator<Face>() {
					@Override
					public int compare(Face o1, Face o2) {
						return o1.compareTo2(o2);
					}
				});
				for(int i = 0; i < listeFaces.size(); i++){
					if(i % etape == 0 && green < 255 && red < 255)
						tmp = degradeColor();
					map.put(listeFaces.get(i), tmp);
				}
				green = blue = 0;
				red = rd.nextInt(256);
				paintComponent(getGraphics());
			}
		});
		
		// Bouton couleur aleatoire
		jMenuItems.get(8).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				map = null;
				color = randomColor();
				paintComponent(getGraphics());
			}
		});
		
		// Bouton toutes couleurs aleatoires
		jMenuItems.get(9).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				map = new HashMap<Face, Color>();
				
				for(int i = 0; i < listeFaces.size(); i++)
					map.put(listeFaces.get(i), randomColor());
				paintComponent(getGraphics());
			}
		});
		
		// Bouton changer wallpaper
		jMenuItems.get(10).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fileopen = null;
				try {
					fileopen = new JFileChooser(new File( "." ).getCanonicalPath());
				} catch (IOException e) {}
				FileFilter filter = new FileNameExtensionFilter(".bmp", "bmp");
				fileopen.addChoosableFileFilter(filter);
				filter = new FileNameExtensionFilter(".jpg", "jpg");
				fileopen.addChoosableFileFilter(filter);

				int ret = fileopen.showDialog(null, "Open file");
				if (ret == JFileChooser.APPROVE_OPTION)
					Data.WALLPAPER = Toolkit.getDefaultToolkit().getImage(fileopen.getSelectedFile().getAbsolutePath());
			}
		});
		
		// Bouton spin
		jMenuItems.get(11).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				for(int i = 0; i < 51; i++){
					new Rotation(listePoints, listeFaces, Math.toRadians(10), "X");
					new Rotation(listePoints, listeFaces, Math.toRadians(10), "Y");
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					paintComponent(getGraphics());
				}
			}
		});
		
		// Bouton qu'est ce que c'est ?
		jMenuItems.get(12).addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFrame jf = new JFrame("A propos");
				jf.setSize(890, 500);
				jf.setLocationRelativeTo(null);
				jf.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				jf.setIconImage(Data.ICON3D);
				JTextArea jta = new JTextArea();
				FileReader flux= null;
				BufferedReader input= null;
				String str;
				try{ 
					jta.setEditable(false);
					flux = new FileReader(Data.FICHIER_AIDE);
					input = new BufferedReader(flux);
					while((str=input.readLine())!=null)
						jta.append(str+"\n");
				} 
				catch (IOException e) {
					JOptionPane.showMessageDialog(null, "erreur fichier manquant", "erreur", JOptionPane.ERROR_MESSAGE);
				}
				finally{
					try {
						flux.close();
					} catch (IOException ex) {
						JOptionPane.showMessageDialog(null, "fichier ne peut etre ferme", "erreur", JOptionPane.ERROR_MESSAGE);
					}
				}
				jf.setContentPane(jta);
				jf.setVisible(true);
			}
		});
	}

	protected Color degradeColor() {
		return new Color(red, green++, blue++);
	}
	
	protected void editSize() {
		JLabel taille = new JLabel("Entrez une taille : ");
		JButton ok = new JButton("Valider");
		final JFrame editSize = new JFrame();
		Font f = new Font("Arial", Font.PLAIN, 16);
		final JTextField jtf = new JTextField();
		
		taille.setPreferredSize(new Dimension(200, 40));
		ok.setPreferredSize(new Dimension(200, 40));
		jtf.setPreferredSize(new Dimension(200, 40));
		taille.setFont(f);
		ok.setFont(f);
		editSize.setLayout(new BoxLayout(editSize.getContentPane(), BoxLayout.Y_AXIS));
		editSize.add(taille);
		editSize.add(jtf);
		editSize.add(ok);
		editSize.setLocationRelativeTo(null);
		editSize.setSize(400, 120);
		editSize.setVisible(true);
		jtf.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					tailleSegment = Integer.parseInt(jtf.getText());
				}
				catch (Exception ex){
					tailleSegment = -1;
				}
				editSize.dispose();
			}
		});
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					tailleSegment = Integer.parseInt(jtf.getText());
				}
				catch (Exception ex){
					tailleSegment = -1;
				}
				editSize.dispose();
			}
		});
	}

	protected void setFullScreen() {
		GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

		if(fullScreen){
			device.setFullScreenWindow(null);
			setExtendedState(JFrame.MAXIMIZED_BOTH);
			fullScreen = false;
		}
		else{
			device.setFullScreenWindow(this);
			fullScreen = true;
		}
	}

	protected void exportImage() {
		export = true;
		new ExportFile(this, decalX, decalY);
		export = false;		
	}

	private Color randomColor(){
		return new Color(rd.nextInt(256), rd.nextInt(256), rd.nextInt(256));
	}

	private void updateModel(){
		String[] tmp = modele.split("\\\\");
		setTitle(Data.TITLE + " - " + tmp[tmp.length-1].toLowerCase());
		reader = new GtsReader(100, modele);
		listeFaces = reader.getListFaces();
		listePoints = reader.getListPoint();
		listeSegments = reader.getListSegments();
		map = null;
		color = new Color(100, 100, 100);
		Data.alphaX = Data.alphaY = 0; // recentrage de la figure
		paintComponent(getGraphics());
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Graphics offgc;
		Image offscreen = null;
		Color tmp;
		
		decalX = listeModeles.getFixedCellWidth() + 27;
		decalY = 52;
		if(export)
			decalX = decalY = 0;
		
		offscreen = createImage(this.getWidth() - decalX, this.getHeight() - decalY);
		if(offscreen != null){
			offgc = offscreen.getGraphics();
			
			// Dessin des faces
			double scal = 0.0; // Produit scalaire pour la lumiere
			
			offgc.drawImage(Data.WALLPAPER, 0, 0, this);
			if(modele != null){
				if(moving){
					offgc.setColor(Color.BLACK);
					for (Segment s : listeSegments)
						offgc.drawLine(s.getSegment1(), s.getSegment2(), s.getSegment3(), s.getSegment4());
				}
				else {
					for (Face f : listeFaces) {
						scal = Math.abs(Data.LUMIERE.prodScalaire(f.getNormal()));

						if(map == null)
							offgc.setColor((new Color((int)(color.getRed() * scal), (int)(color.getGreen() * scal), (int)(color.getBlue() * scal))));
						else{
							tmp = map.get(f);
							offgc.setColor(new Color((int)(tmp.getRed() * scal), (int)(tmp.getGreen() * scal), (int)(tmp.getBlue() * scal)));
						}
						offgc.fillPolygon(f.getTriangleX(), f.getTriangleY(), 3);
					}
				}
			}
			g2.drawImage(offscreen, decalX, decalY, this);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		double zoom;
		
		if(modele != null){
 			if (e.getWheelRotation() < 0)
				zoom = 1.1;
			else
				zoom = 0.9;
			new Homothetie(listePoints, zoom);
			paintComponent(getGraphics());
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void mouseDragged(MouseEvent e) {
		if(modele != null){
			if (SwingUtilities.isLeftMouseButton(e)) {
				setCursor(Cursor.HAND_CURSOR);
				moving = true;
				Point p = new Point(e.getX(), e.getY(), 0);
				
				cptMouse++;
				if (cptMouse == 4) {
					if (p.getY() < current.getY())
						new Rotation(listePoints, listeFaces, Math.toRadians(10), "X");
					if (p.getX() < current.getX())
						new Rotation(listePoints, listeFaces, Math.toRadians(10), "Y");
					if (p.getY() > current.getY())
						new Rotation(listePoints, listeFaces, Math.toRadians(-10), "X");
					if (p.getX() > current.getX())
						new Rotation(listePoints, listeFaces, Math.toRadians(-10), "Y");
					current = p;
					cptMouse = 0;
					paintComponent(getGraphics());
				}
			} 
			else if (SwingUtilities.isRightMouseButton(e)) {
				setCursor(Cursor.MOVE_CURSOR);
				moving = true;
				Point p = new Point(e.getX(), e.getY(), 0);
	
				if (p.getY() < current.getY())
					new Translation(listePoints, -5, "Y");
				if (p.getY() > current.getY())
					new Translation(listePoints, 5, "Y");
				if (p.getX() > current.getX())
					new Translation(listePoints, 5, "X");
				if (p.getX() < current.getX())
					new Translation(listePoints, -5, "X");
				current = p;
				paintComponent(getGraphics());
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		setFocusable(true);
		filtre.setFocusable(false);
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if(modele != null){
			if (current == null)
				current = new Point(e.getX(), e.getY(), 0);
			else {
				current.setX(e.getX());
				current.setY(e.getY());
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void mouseReleased(MouseEvent arg0) {
		setCursor(Cursor.DEFAULT_CURSOR);
		moving = false;
		paintComponent(getGraphics());
	}
	@Override
	public void keyPressed(KeyEvent e) {
		if(modele != null){
			if (e.getKeyCode() == KeyEvent.VK_Z)
				new Rotation(listePoints, listeFaces, Math.toRadians(10), "X");
			if (e.getKeyCode() == KeyEvent.VK_Q)
				new Rotation(listePoints, listeFaces, Math.toRadians(10), "Y");
			if (e.getKeyCode() == KeyEvent.VK_S)
				new Rotation(listePoints, listeFaces, Math.toRadians(-10), "X");
			if (e.getKeyCode() == KeyEvent.VK_D)
				new Rotation(listePoints, listeFaces, Math.toRadians(-10), "Y");
			paintComponent(getGraphics());
		}
	}
	@Override
	public void keyReleased(KeyEvent arg0) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
}