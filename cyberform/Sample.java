import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.event.*;

import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.universe.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.io.*;
import java.net.*;

public class Sample extends Applet {

	public BranchGroup createSceneGraph() {
		BranchGroup sgRoot = new BranchGroup();
		
		Transform3D	t3d = new Transform3D();
		
		TransformGroup objTrans = new TransformGroup();
		t3d.setIdentity();
		t3d.setScale(0.018f);
		t3d.setTranslation(new Vector3d(0.0f, -0.2f, 0.0f));
		objTrans.setTransform(t3d);
		sgRoot.addChild(objTrans);

		TransformGroup objRot = new TransformGroup();
		objRot.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		objRot.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		objTrans.addChild(objRot);

		CyberFormGeom geom = new CyberFormGeom();
		for (int n=0; n<geom.getShapeCount(); n++)
			objRot.addChild(geom.getShape(n));

		BoundingSphere bounds = new BoundingSphere(new Point3d(0.0,0.0,0.0), Double.MAX_VALUE);

		Transform3D yAxis = new Transform3D();
		Alpha rotationAlpha = new Alpha(-1, Alpha.INCREASING_ENABLE, 0, 0, 4000, 0, 0, 0, 0, 0);

		RotationInterpolator rotator = new RotationInterpolator(rotationAlpha, objRot, yAxis, 0.0f, (float)Math.PI*2.0f);
		rotator.setSchedulingBounds(bounds);
		objRot.addChild(rotator);
	  
		Background bgNode = new Background(new Color3f(0.0f, 0.0f, 0.0f));
		bgNode.setApplicationBounds(bounds);
		sgRoot.addChild(bgNode);

		AmbientLight ambientLightNode = new AmbientLight(new Color3f(0.5f, 0.5f, 0.5f));
		ambientLightNode.setInfluencingBounds(bounds);
		sgRoot.addChild(ambientLightNode);

		DirectionalLight light1 = new DirectionalLight(new Color3f(1.0f, 1.0f, 1.0f), new Vector3f(0.0f, 0.0f, -1.0f));
		light1.setInfluencingBounds(bounds);
		sgRoot.addChild(light1);

		return sgRoot;
    }

	public void init() {
		System.out.println("Applet.init()");

		setLayout(new BorderLayout());
		Canvas3D c = new Canvas3D(null);
		add("Center", c);

		BranchGroup scene = createSceneGraph();
		SimpleUniverse u = new SimpleUniverse(c);
		u.getViewingPlatform().setNominalViewingTransform();
		u.addBranchGraph(scene);
	}

	public static void main(String[] args) {
		new MainFrame(new Sample(), 300, 300);
	}
}
