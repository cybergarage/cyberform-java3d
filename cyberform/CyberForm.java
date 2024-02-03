/******************************************************************
*
*	CyberForm for Java3D
*
*	Copyright (C) Satoshi Konno 1999
*
*	File:	CyberForm.java
*
******************************************************************/

import java.awt.*;
import java.applet.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.loaders.*;
import com.sun.j3d.loaders.lw3d.Lw3dLoader;

import cv97.*;
import cv97.node.*;
import cv97.node.Node;
import cv97.field.*;
import cv97.j3d.*;

public class CyberForm extends JFrame implements Constants, MouseListener, MouseMotionListener, KeyListener, Runnable {

	private final static String RELEASE_NUMBER		= "1.2";
	private final static String RELEASE_DATE			= "2000/03/31";
	
	private SceneGraph				mSceneGraph			= null;				
	private TransformNode			mTopTransformNode	= null;
	private Thread						mThread				= null;
	
	private PrintFrame				mPrintFrame 		= null;
	
	private int							mCoordMode				= Java3DSaver.COORD_16;
	private int							mNormalMode				= Java3DSaver.NORMAL_8;
	private int							mColorMode				= Java3DSaver.COLOR_RGBA16;
	private int							mTextureColorMode		= Java3DSaver.TEXTURE_TARGA_RGBA16;
	private int							mTexCoordMode			= Java3DSaver.TEXCOORD_8;
	private int							mIndexMode				= Java3DSaver.INDEX_16;
		
	public void setSceneGraph(SceneGraph sg) {
		mSceneGraph = sg;
	}

	public SceneGraph getSceneGraph() {
		return mSceneGraph;
	}

	public void setTopTransformNode(TransformNode transNode) {
		mTopTransformNode = transNode; 
	}

	public TransformNode getTopTransformNode() {
		return mTopTransformNode;
	}

	private Frame getParentFrame() {
		return this;
	}

	private void setCoordModeOption(int mode) {
		mCoordMode = mode;
	}

	private int getCoordModeOption() {
		return mCoordMode;
	}

	private void setNormalModeOption(int mode) {
		mNormalMode = mode;
	}

	private int getNormalModeOption() {
		return mNormalMode;
	}

	private void setColorModeOption(int mode) {
		mColorMode = mode;
	}

	private int getColorModeOption() {
		return mColorMode;
	}

	private void setIndexModeOption(int mode) {
		mIndexMode = mode;
	}

	private int getIndexModeOption() {
		return mIndexMode;
	}
		
	private void setTextureColorModeOption(int mode) {
		mTextureColorMode = mode;
	}

	private int getTextureColorModeOption() {
		return mTextureColorMode;
	}

	private void setTexCoordModeOption(int mode) {
		mTexCoordMode = mode;
	}

	private int getTexCoordModeOption() {
		return mTexCoordMode;
	}

	public CyberForm(){
		super("CyberForm");
		
		enableEvents(AWTEvent.MOUSE_EVENT_MASK);
		enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
		
		getContentPane().setLayout(new BorderLayout());
		
		Canvas3D canvas3d = new Canvas3D(null);

		canvas3d.addMouseListener(this);
		canvas3d.addMouseMotionListener(this);

		SceneGraph sg = new SceneGraph();
		setSceneGraph(sg);
		sg.setObject(new SceneGraphJ3dObject(canvas3d, sg));
		clearSceneGraph();
				
		getContentPane().add("North",  createToolBar());
		getContentPane().add("Center", canvas3d);

		addWindowListener(new QuitAction());
		addKeyListener(this);
		
		setSize(320,320 + 32);
		show();
	}

	private void clearSceneGraph() {
		stopSimulation();
		SceneGraph sg = getSceneGraph();
		sg.clear();
		TransformNode topTransNode = new TransformNode();
		setTopTransformNode(topTransNode);
		sg.addNode(topTransNode);
		sg.initialize();
		startSimulation();
	}
	
	public void start() {
		if (mThread == null) {
			mThread = new Thread(this);
			mThread.start();
		}
	}

	public void stop() {
		if (mThread != null) {
			mThread.stop();
			mThread = null;
		}
	}

	////////////////////////////////////////////////
	//	Toolbar
	////////////////////////////////////////////////

	private JButton createToolBarButton(String name, String imgFileName, Action action) {
		JButton b = new JButton(new ImageIcon(imgFileName, name));
		b.setToolTipText(name);
		b.setMargin(new Insets(0,0,0,0));
		if (action != null)
			b.addActionListener(action);
		return b;
	}
	
	private JToolBar createToolBar() {
		JToolBar toolBar = new JToolBar();

		toolBar.setBorderPainted(true);
		toolBar.setFloatable(false);
		
		toolBar.add(createToolBarButton("Load", "images/load.gif",     new LoadAction()));
		toolBar.add(createToolBarButton("Save", "images/save.gif",     new SaveAction()));
		toolBar.addSeparator();
		toolBar.add(createToolBarButton("Config", "images/config.gif", new ConfigAction()));
		toolBar.addSeparator();
		toolBar.add(createToolBarButton("About", "images/about.gif",   new AboutAction()));
		
		return toolBar;
	}
	
	////////////////////////////////////////////////
	//	FileFilter
	////////////////////////////////////////////////

	public class VRML97FileFilter extends FileFilter {
		final static String ext = "wrl";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "VRML97 Files (*.wrl)";
		}
	}

	public class A3DSFileFilter extends FileFilter {
		final static String ext = "3ds";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "3DSutdio Files (*.3ds)";
		}
	}

	public class OBJFileFilter extends FileFilter {
		final static String ext = "obj";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "OBJ Files (*.obj)";
		}
	}

	public class NFFFileFilter extends FileFilter {
		final static String ext = "nff";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "NFF Files (*.nff)";
		}
	}

	public class LW3DFileFilter extends FileFilter {
		final static String ext = "lws";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "LightWave3D Files (*.lws)";
		}
	}

	public class DXFFileFilter extends FileFilter {
		final static String ext = "dxf";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) 
					return true;
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "DXF Files (*.dxf)";
		}
	}

	public class STLFileFilter extends FileFilter {
		final static String ext1 = "stl";
		final static String ext2 = "slp";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext1.equals(extension) == true) 
					return true;
				if (ext2.equals(extension) == true) 
					return true;
				return false;
			}
			return false;
		}
		public String getDescription() {
			return "STL Files (*.stl, *.slp)";
		}
	}

	public class X3DFileFilter extends FileFilter {
		final static String ext1 = "x3d";
		final static String ext2 = "xml";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext1.equals(extension) == true) 
					return true;
				if (ext2.equals(extension) == true) 
					return true;
				return false;
			}
			return false;
		}
		public String getDescription() {
			return "X3D Files (*.x3d, *.xml)";
		}
	}

	public class JavaSourceFileFilter extends FileFilter {
		final static String ext = "java";
		public boolean accept(File f) {
			if(f.isDirectory())
				return true;
			String s = f.getName();
			int i = s.lastIndexOf('.');
			if(i > 0 &&  i < s.length() - 1) {
				String extension = s.substring(i+1).toLowerCase();
				if (ext.equals(extension) == true) {
					if (s.equals(Java3DSaver.getGeometryLoaderFileName()) == true)
						return false;
					return true;
				}
				else
					return false;
			}
			return false;
		}
		public String getDescription() {
			return "Java Source Files (*.java)";
		}
	}

	////////////////////////////////////////////////
	//	LoadAction
	////////////////////////////////////////////////
	
	private class LoadAction extends AbstractAction {
		
		public void loadLW3DGeometryFile(File file) {
			Loader lw3dLoader = new Lw3dLoader(Loader.LOAD_ALL);
			Scene loaderScene = null;
			try {
				loaderScene = lw3dLoader.load(file.toURL());
				Hashtable sceneHash = loaderScene.getNamedObjects();
				
				BranchGroup root = new BranchGroup();
				for (Enumeration e = sceneHash.elements(); e.hasMoreElements() ;) {
					TransformGroup obj = (TransformGroup)e.nextElement();
					System.out.println("Object = " + obj);
					if (obj.getParent() != null) {
						Group parentGroup = (Group)obj.getParent();
						int numChildren = parentGroup.numChildren(); 
						for (int n=0; n<numChildren; n++) {
							if (parentGroup.getChild(n) == obj) {
								parentGroup.removeChild(n);
								break;
							}
						}
					}
					root.addChild(obj);
				}
				
				VRML97Saver vrmlSaver = new VRML97Saver();
				vrmlSaver.setBranchGroup(root);
				
				Node node = vrmlSaver.getNodes();
				while (node != null) {
					Node nextNode = node.next();
					getTopTransformNode().addChildNode(node);
					node = nextNode;
				}
			}
			catch (Exception e) {
				System.err.println("Exception loading file: " + e);
				Message.showWarningDialog(getParentFrame(), "Couldn't load a specified file (" + file.toString() + ")");
				return;
			}
		}
		
		public void loadGeomrtryFile(File file) {
			clearSceneGraph();
			
			int format = SceneGraph.getFileFormat(file.toString());
			if (format != SceneGraph.FILE_FORMAT_LWS && format != SceneGraph.FILE_FORMAT_LWO) {
				SceneGraph sgTmp = new SceneGraph();
				if (sgTmp.load(file) == true) {
					getSceneGraph().setBaseURL(sgTmp.getBaseURL());
					Node node = sgTmp.getNodes();
					while (node != null) {
						Node nextNode = node.next();
						getTopTransformNode().addChildNode(node);
						node = nextNode;
					}
				}
				else
					Message.showWarningDialog(getParentFrame(), "Couldn't load a specified file (" + file.toString() + ")");
			}
			else 
				loadLW3DGeometryFile(file);

			// Reset Viewpoint positon
			SceneGraph sg = getSceneGraph();
			sg.initialize();
			sg.resetViewpointAlongZAxis();

			// Turn on headlight 			
			NavigationInfoNode navInfo = sg.getNavigationInfoNode();
			if (navInfo == null)
				navInfo = sg.getDefaultNavigationInfoNode();
			navInfo.setHeadlight(true);
		}
		
		public void actionPerformed(ActionEvent e) {
			stopSimulation();
			
			String userDir = System.getProperty("user.dir");
			
			JFileChooser filechooser = new JFileChooser(new File(userDir));
			filechooser.setDialogTitle("Load");
			FileFilter vrml97fileFilter = new VRML97FileFilter();
			filechooser.addChoosableFileFilter(vrml97fileFilter);
			filechooser.addChoosableFileFilter(new A3DSFileFilter());
			filechooser.addChoosableFileFilter(new OBJFileFilter());
			filechooser.addChoosableFileFilter(new NFFFileFilter());
			filechooser.addChoosableFileFilter(new LW3DFileFilter());
			filechooser.addChoosableFileFilter(new DXFFileFilter());
			filechooser.addChoosableFileFilter(new STLFileFilter());
			filechooser.addChoosableFileFilter(new X3DFileFilter());
			filechooser.setFileFilter(vrml97fileFilter);
			
			if(filechooser.showOpenDialog(getParentFrame()) == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				if (file != null) {
					if (file.isDirectory() == false) {
						setWaitCursor();
						loadGeomrtryFile(file);
						setNormalCursor();
					}
				}
			}
			
			startSimulation();
		}
	}

	////////////////////////////////////////////////
	//	SaveAction
	////////////////////////////////////////////////

	private class SaveAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			JPopupMenu.setDefaultLightWeightPopupEnabled(false); 
			PopupMenu popupMenu = new PopupMenu();
			Component comp = (Component)e.getSource();
			Dimension size = comp.getSize();
			popupMenu.show(comp, size.width/2, size.height/2);
		}

		public void saveAsJava3D() {
			stopSimulation();
			
			SceneGraph sg = getSceneGraph();
			
			getTopTransformNode().setRotation(0, 0, 1, 0);	
			
			String userDir = System.getProperty("user.dir");
			JFileChooser filechooser = new JFileChooser(new File(userDir));
			FileFilter fileFilter = new JavaSourceFileFilter();
			filechooser.addChoosableFileFilter(fileFilter);
			filechooser.setFileFilter(fileFilter);
			filechooser.setDialogTitle("Save as Java Source");
			if(filechooser.showSaveDialog(getParentFrame()) == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				if (file != null) {
					if (file.isDirectory() == false) {
						setWaitCursor();
						Java3DSaver j3dSaver = new Java3DSaver(sg);
						j3dSaver.save(file, getCoordModeOption(), getNormalModeOption(), getColorModeOption(), getTextureColorModeOption(), getTexCoordModeOption());			
						setNormalCursor();
					}
				}
			}
			
			startSimulation();
		}

		public void saveAsVRML97() {
			stopSimulation();
			
			SceneGraph sg = getSceneGraph();
			
			getTopTransformNode().setRotation(0, 0, 1, 0);	
			
			String userDir = System.getProperty("user.dir");
			JFileChooser filechooser = new JFileChooser(new File(userDir));
			FileFilter fileFilter = new VRML97FileFilter();
			filechooser.addChoosableFileFilter(fileFilter);
			filechooser.setFileFilter(fileFilter);
			filechooser.setDialogTitle("Save as VRML97");
			if(filechooser.showSaveDialog(getParentFrame()) == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				if (file != null) {
					if (file.isDirectory() == false) {
						setWaitCursor();
						sg.save(file);			
						setNormalCursor();
					}
				}
			}
			
			startSimulation();
		}
		
		public class PopupMenu extends JPopupMenu {
			private String menuString[] = {
				"Save as Java Source",
				"Save as VRML97",
			};
		
			public PopupMenu() {
				for (int n=0; n<menuString.length; n++) {
					JMenuItem menuItem = new JMenuItem(menuString[n]);
					menuItem.addActionListener(new PopupMenuAction());
					add(menuItem);
				}
			}
		
			private class PopupMenuAction extends AbstractAction {
  			 	public void actionPerformed(ActionEvent e) {
					for (int n=0; n<menuString.length; n++) {
						if (menuString[n].equals(e.getActionCommand()) == true) {
							switch (n) {
							case 0: saveAsJava3D(); break;
							case 1: saveAsVRML97(); break;
							}
							break;
						}
					}
				}
			}
		}
	}

	////////////////////////////////////////////////
	//	ConfigAction
	////////////////////////////////////////////////
	
	private void setWaitCursor() {
		getParentFrame().setCursor(Cursor.WAIT_CURSOR);
	}

	private void setNormalCursor() {
		getParentFrame().setCursor(Cursor.DEFAULT_CURSOR);
	}
	
	////////////////////////////////////////////////
	//	ConfigAction
	////////////////////////////////////////////////
	
	private class ConfigAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			ConfigDialog configDialog = new ConfigDialog(getParentFrame());
			if (configDialog.doModal() == Dialog.OK_OPTION) {
				setCoordModeOption(configDialog.getCoordMode());
				setNormalModeOption(configDialog.getNormalMode());
				setColorModeOption(configDialog.getColorMode());
				setTextureColorModeOption(configDialog.getTextureColorMode());
				setTexCoordModeOption(configDialog.getTexCoordMode());
				setIndexModeOption(configDialog.getIndexMode());
			}
		}
	}

	////////////////////////////////////////////////
	//	AboutAction
	////////////////////////////////////////////////
	
	private class AboutAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			Dialog aboutDialog = new AboutDialog(getParentFrame());
			aboutDialog.doModal();
		}
	}

	////////////////////////////////////////////////
	//	QuitAction
	////////////////////////////////////////////////
	
	private class QuitAction extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.exit(0);
		}
	}

	//////////////////////////////////////////////////
	// Simulation
	//////////////////////////////////////////////////

	public void startSimulation() {
		SceneGraph sg = getSceneGraph();
		if (sg.isSimulationRunning() == false) {
			sg.initialize();
			sg.startSimulation();
			start();
		}
	}

  	public void stopSimulation() {
		SceneGraph sg = getSceneGraph();
		if (sg.isSimulationRunning() == true) {
			sg.stopSimulation();
			stop();
		}
	}
	
	////////////////////////////////////////////////
	//	mouse
	////////////////////////////////////////////////

	private int	mMouseX = 0;
	private int	mMouseY = 0;
	private int	mMouseButton = 0;

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.getModifiers() == e.BUTTON1_MASK)
			mMouseButton = 1;
	}

	public void mouseReleased(MouseEvent e) {
		if (e.getModifiers() == e.BUTTON1_MASK)
			mMouseButton = 0;
	}

	public void mouseDragged(MouseEvent e) {
		mMouseX = e.getX();
		mMouseY = e.getY();
	}

	public void mouseMoved(MouseEvent e) {
		mMouseX = e.getX();
		mMouseY = e.getY();
	}

	public int getMouseX() {
		return mMouseX;
	}

	public int getMouseY() {
		return mMouseY;
	}

	public int getMouseButton() {
		return mMouseButton;
	}

	////////////////////////////////////////////////
	//	Viewpoint
	////////////////////////////////////////////////
	
	SFRotation xrot		= new SFRotation();
	SFRotation yrot		= new SFRotation();
	SFRotation transRot	= new SFRotation();
	
	public void updateViewpoint() {
		// get mouse infomations
		float	width2 = (float)getWidth() / 2.0f;
		float	height2 = (float)getHeight() /2.0f;

		int		mx = getMouseX();
		int		my = getMouseY();
		int		mbutton = getMouseButton();
		
		float	xangle = 0.0f;
		float	yangle = 0.0f;

		if (mbutton == 1) {
			xangle = ((float)my - height2) / height2 * (float)Math.PI;
			yangle = ((float)mx - width2) / width2 * (float)Math.PI;
		}
		xrot.setValue(1, 0, 0, xangle);
		yrot.setValue(0, 1, 0, yangle);
		
		transRot.setValue(xrot);
		transRot.add(yrot);
		getTopTransformNode().setRotation(transRot.getX(), transRot.getY(), transRot.getZ(), transRot.getAngle());
	}

	////////////////////////////////////////////////
	//	runnable
	////////////////////////////////////////////////

	public void run() {
		while (true) {
			updateViewpoint();
			repaint();
			try {
				mThread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}

	////////////////////////////////////////////////
	//	Dialog
	////////////////////////////////////////////////
	
	public class Dialog extends JDialog {

		public static final int	OK_OPTION = 0;
		public static final int	CANCEL_OPTION = -1;
		
		private JDialog			mDialog = null;
		private Object				mOptions[] = null;
		private int					mValue = CANCEL_OPTION;
		private boolean			mOnlyOKButton = false;
		private ComponentPanel	mComponentPanel = null;
		
		public Dialog(Frame parentComponent, String title, JComponent components[]) {
			super(parentComponent, title, true);
			setComponents(components);
			setLocationRelativeTo(parentComponent);
			setValue(CANCEL_OPTION);
		}

		public Dialog(Frame parentComponent, String title, boolean onlyOKButton) {
			super(parentComponent, title, true);
			setLocationRelativeTo(parentComponent);
			setValue(CANCEL_OPTION);
			mOnlyOKButton = onlyOKButton;		
		}

		public Dialog(Frame parentComponent, String title) {
			super(parentComponent, title, true);
			setLocationRelativeTo(parentComponent);
			setValue(CANCEL_OPTION);
		}

		public Dialog(Frame parentComponent) {
			super(parentComponent, "", true);
			setLocationRelativeTo(parentComponent);
			setValue(CANCEL_OPTION);
		}
		
		public void setComponents(JComponent components[]) {
			mComponentPanel = new ComponentPanel(components, new BoxLayout(this, BoxLayout.Y_AXIS));
			getContentPane().add(mComponentPanel);
		}

		public void setComponents(JComponent components[], LayoutManager layoutMgr) {
			mComponentPanel = new ComponentPanel(components, layoutMgr);
			getContentPane().add(mComponentPanel);
		}
		
		public void setPreferredSize(Dimension dim) {
			mComponentPanel.setPreferredSize(dim);
		}
		
		public boolean isOnlyOKButton() {
			return mOnlyOKButton;
		}
		
		private class ComponentPanel extends JPanel {
			
			private ConfirmComponent mConfirmComponent;
			
			public ComponentPanel(JComponent components[], LayoutManager layoutMgr) {
				setBorder(new EmptyBorder(5, 5, 0, 5));
				setLayout(layoutMgr);
				for (int n=0; n<components.length; n++)
					add(components[n]);
				setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
				mConfirmComponent = new ConfirmComponent(); 
				add(mConfirmComponent);
			}
			
			public ConfirmComponent getComfirmComponent() {
				return mConfirmComponent;
			}
		}
		
		private class ConfirmComponent extends JPanel {
			
			private OkButton		mOkButton;
			private CancelButton	mCancelButton;
			
			public ConfirmComponent() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
				setBorder(new EmptyBorder(5, 5, 5, 5));
				
				mOkButton = new OkButton();
				add(mOkButton);
				if (isOnlyOKButton() == false) {
					mCancelButton = new CancelButton();
					add(mCancelButton);
				}
				//add(new OkButton(new ImageIcon(getUserDir() + "ok.gif", "OK")));
				//add(new CancelButton(new ImageIcon(getUserDir() + "cancel.gif", "Cancel")));
			}

			public String getUserDir() {
				String separator = System.getProperty("file.separator");
				return System.getProperty("user.dir") + separator + "images" + separator + "dialog" + separator;
			}
				
			private class OkButton extends JButton implements ActionListener {
				public OkButton() {
					super("OK");
					addActionListener(this);
				}
				public OkButton(Icon icon) {
					super(icon);
					addActionListener(this);
				}
				public void actionPerformed(ActionEvent e) {
					setValue(OK_OPTION);
					dispose();
				}
			}

			private class CancelButton extends JButton implements ActionListener {
				public CancelButton() {
					super("Cancel");
					addActionListener(this);
				}
				public CancelButton(Icon icon) {
					super(icon);
					addActionListener(this);
				}
				public void actionPerformed(ActionEvent e) {
					setValue(CANCEL_OPTION);
					dispose();
				}
			}
			
			public void setOkButtonEnabled(boolean on) {
				mOkButton.setEnabled(on);
			}

			public boolean isOkButtonEnabled() {
				return mOkButton.isEnabled();
			}
	 	}
		
		public void setOkButtonEnabled(boolean on) {
			if (mComponentPanel == null)
				return;
			mComponentPanel.getComfirmComponent().setOkButtonEnabled(on);		
		}

		public boolean isOkButtonEnabled() {
			if (mComponentPanel == null)
				return false;
			return mComponentPanel.getComfirmComponent().isOkButtonEnabled();		
		}
		
		public void setValue(int value) {
			mValue = value;
		}
		
		public int getValue() {
			return mValue;
		}
		
		public int doModal() {
			pack();
			setVisible(true);
			return getValue();
		}

		public int doModal(int width, int height) {
			setSize(width, height);
			pack();
			setVisible(true);
			return getValue();
		}
	}
	
	////////////////////////////////////////////////
	//	AboutDialog
	////////////////////////////////////////////////
	
	public class AboutDialog extends Dialog {
	
		public AboutDialog(Frame parentFrame) {
			super(parentFrame, "About", true);
			JComponent dialogComponent[] = new JComponent[5];
			dialogComponent[0] = new CyberToolboxPanel();
			dialogComponent[1] = new ReleaseDate();
			dialogComponent[2] = new CopyRightPanel();
			dialogComponent[3] = new WebPanel();
			dialogComponent[4] = new MailPanel();
			setComponents(dialogComponent);
		}

		private class CyberToolboxPanel extends JPanel {
			public CyberToolboxPanel() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));
				setBorder(new EmptyBorder(5, 5, 5, 5));
				add(new JLabel("CyberForm for Java3D Release " + RELEASE_NUMBER));
			}
 		}

		private class ReleaseDate extends JPanel {
			public ReleaseDate() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10,2));
//				setBorder(new EmptyBorder(5, 5, 5, 5));
				add(new JLabel("Release Date : " + RELEASE_DATE));
			}
 		}

		private class CopyRightPanel extends JPanel {
			public CopyRightPanel() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2));
//				setBorder(new EmptyBorder(5, 5, 5, 5));
				add(new JLabel("Copyright (C) 1999 Satoshi Konno, All right reserved."));
			}
 		}

		private class WebPanel extends JPanel {
			public WebPanel() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2));
//				setBorder(new EmptyBorder(5, 5, 5, 5));
				add(new JLabel("http://www.cyber.koganei.tokyo.jp"));
			}
 		}

		private class MailPanel extends JPanel {
			public MailPanel() {
				setLayout(new FlowLayout(FlowLayout.CENTER, 10, 2));
//				setBorder(new EmptyBorder(5, 5, 5, 5));
				add(new JLabel("skonno@cyber.koganei.tokyo.jp"));
			}
 		}
	}

	////////////////////////////////////////////////
	//	ConfigDialog
	////////////////////////////////////////////////

	public class ConfigDialog extends Dialog {
		
		JCheckBox	mUseInnerClassCheckBox;
		JCheckBox 	mUsePackedDataCheckBox;
		JComboBox	mCoordModeComobox;	
		JComboBox	mNormalModeComobox;	
		JComboBox	mColorModeComobox;	
		JComboBox	mTextureModeComobox;	
		JComboBox	mTexCoordModeComobox;	
		JComboBox	mIndexModeComobox;	
			
		public ConfigDialog(Frame parentFrame) {
			super(parentFrame, "Config");
			
			JPanel mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			mainPanel.setBorder(BorderFactory.createTitledBorder("Generation Option"));

			mCoordModeComobox = new JComboBox();
			mCoordModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "Coordinate Precision")));
			mCoordModeComobox.addItem("32bit");
			mCoordModeComobox.addItem("24bit");
			mCoordModeComobox.addItem("16bit");
			mCoordModeComobox.setSelectedIndex(getCoordModeOption());
			mainPanel.add(mCoordModeComobox);

			mNormalModeComobox = new JComboBox();
			mNormalModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "Normal Precision")));
			mNormalModeComobox.addItem("32bit");
			mNormalModeComobox.addItem("24bit");
			mNormalModeComobox.addItem("16bit");
			mNormalModeComobox.addItem(" 8bit");
			mNormalModeComobox.setSelectedIndex(getNormalModeOption());
			mainPanel.add(mNormalModeComobox);
		
			mColorModeComobox = new JComboBox();
			mColorModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "Color Format")));
			mColorModeComobox.addItem("RGB  (24bit)");
			mColorModeComobox.addItem("RGBA (32bit)");
			mColorModeComobox.addItem("RGBA (16bit)");
			mColorModeComobox.setSelectedIndex(getColorModeOption());
			mainPanel.add(mColorModeComobox);

			mTextureModeComobox = new JComboBox();
			mTextureModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "Texture Format")));
			mTextureModeComobox.addItem("No Texture");
			mTextureModeComobox.addItem("RGB  (24bit)");
			mTextureModeComobox.addItem("RGBA (32bit)");
			mTextureModeComobox.addItem("RGBA (16bit)");
			mTextureModeComobox.setSelectedIndex(getTextureColorModeOption());
			mainPanel.add(mTextureModeComobox);

			mTexCoordModeComobox = new JComboBox();
			mTexCoordModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "TexCoord Precision")));
			mTexCoordModeComobox.addItem("32bit");
			mTexCoordModeComobox.addItem("24bit");
			mTexCoordModeComobox.addItem("16bit");
			mTexCoordModeComobox.addItem(" 8bit");
			mTexCoordModeComobox.setSelectedIndex(getTexCoordModeOption());
			mainPanel.add(mTexCoordModeComobox);
			
			mIndexModeComobox = new JComboBox();
			mIndexModeComobox.setBorder(new TitledBorder(new TitledBorder(LineBorder.createBlackLineBorder(), "Index Range")));
			mIndexModeComobox.addItem("32bit");
			mIndexModeComobox.addItem("16bit");
			mIndexModeComobox.addItem(" 8bit");
			mIndexModeComobox.setSelectedIndex(getIndexModeOption());
			//mainPanel.add(mIndexModeComobox);

			JComponent dialogComponent[] = new JComponent[1];
			dialogComponent[0] = mainPanel;
			setComponents(dialogComponent);
		}
		
		public int getCoordMode() {
			return mCoordModeComobox.getSelectedIndex();
		}

		public int getNormalMode() {
			return mNormalModeComobox.getSelectedIndex();
		}

		public int getColorMode() {
			return mColorModeComobox.getSelectedIndex();
		}
		
		public int getTextureColorMode() {
			return mTextureModeComobox.getSelectedIndex();
		}

		public int getTexCoordMode() {
			return mTexCoordModeComobox.getSelectedIndex();
		}

		public int getIndexMode() {
			return mIndexModeComobox.getSelectedIndex();
		}

	}

	////////////////////////////////////////////////
	//	PrintFrame
	////////////////////////////////////////////////

	private class PrintFrame extends Frame {
	
		private JTextArea mTextComp;
		
		public PrintFrame() {
			setLayout(new BorderLayout());
			JScrollPane scrollPane = new JScrollPane();
			mTextComp = new PrintTextArea();
			mTextComp.setEditable(false);
			mTextComp.setTabSize(4);
			scrollPane.setViewportView(mTextComp);
			add("Center", scrollPane);
			setSize(320, 240);
			show();
		}
		
		public void setText(String t) {
			mTextComp.setText(t);
		}
	}

	private class PrintTextArea extends JTextArea {
		public PrintTextArea() {
		}
		public void copy() {
		}
		public void cut() {
		}
	}

	private void outputJava3DSource() {
		
		Java3DSaver j3dSaver = new Java3DSaver(getSceneGraph());
		ByteArrayOutputStream byteOutputStrerm = new ByteArrayOutputStream();
		j3dSaver.save(byteOutputStrerm, "SampleGeom", getCoordModeOption(), getNormalModeOption(), getColorModeOption(), getTextureColorModeOption(), getTexCoordModeOption());			
		try {
			byteOutputStrerm.close();
		}
		catch (IOException ioe) {}
		
		if (mPrintFrame == null)
			mPrintFrame = new PrintFrame();
		
		mPrintFrame.setText(byteOutputStrerm.toString());
		mPrintFrame.toFront();
	}
	
	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_P:
			outputJava3DSource(); 
			break;
		case KeyEvent.VK_I:
			break;
		}
	}

	public void keyReleased(KeyEvent e) {
	}
		 
	////////////////////////////////////////////////
	//	main
	////////////////////////////////////////////////
	
	public static void main(String args[]) {
		new CyberForm();
	}
}
