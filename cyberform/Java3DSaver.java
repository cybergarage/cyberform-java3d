/******************************************************************
*
*	CyberForm for Java3D
*
*	Copyright (C) Satoshi Konno 1999
*
*	File:	Java3DSaver.java
*
******************************************************************/

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import cv97.*;
import cv97.node.*;
import cv97.node.Node;
import cv97.field.*;
import cv97.j3d.*;
import cv97.image.*;
import cv97.util.Debug;

public class Java3DSaver extends Object {

	public final static int	X								= 0;
	public final static int	Y								= 1;
	public final static int	Z								= 2;

	public final static int	MIN							= 0;
	public final static int	MAX							= 1;
	
	public final static int COORD_32						= 0;
	public final static int COORD_24						= 1;
	public final static int COORD_16						= 2;

	public final static int NORMAL_32					= 0;
	public final static int NORMAL_24					= 1;
	public final static int NORMAL_16					= 2;
	public final static int NORMAL_8						= 3;

	public final static int COLOR_RGB24					= 0;
	public final static int COLOR_RGBA32				= 1;
	public final static int COLOR_RGBA16				= 2;

	public final static int TEXTURE_NONE				= 0;
	public final static int TEXTURE_TARGA_RGB24		= 1;
	public final static int TEXTURE_TARGA_RGBA32		= 2;
	public final static int TEXTURE_TARGA_RGBA16		= 3;

	public final static int TEXCOORD_32					= 0;
	public final static int TEXCOORD_24					= 1;
	public final static int TEXCOORD_16					= 2;
	public final static int TEXCOORD_8					= 3;

	public final static int INDEX_32						= 0;
	public final static int INDEX_16						= 1;
	public final static int INDEX_8						= 2;

	public final static int GEOM_NONE							= 0;
	public final static int GEOM_INDEXED_TRIANGLE_ARRAY	= 1;
	public final static int GEOM_TRIANGLE_ARRAY				= 2;
	public final static int GEOM_QUAD_ARRAY					= 3;
		
	public Java3DSaver(SceneGraph sg) {
		setSceneGraph(sg);
	}

	///////////////////////////////////////////////
	//	SceneGraph
	///////////////////////////////////////////////
	
	private SceneGraph	mSceneGraph	= null;				
	
	private void setSceneGraph(SceneGraph sg) {
		mSceneGraph = sg;
	}

	private SceneGraph getSceneGraph() {
		return mSceneGraph;
	}

	///////////////////////////////////////////////
	//	ClassName
	///////////////////////////////////////////////
	
	private String			mClassName			= null;				
	
	private void setClassName(String className) {
		mClassName = className;
	}

	private String getClassName() {
		return mClassName;
	}

	///////////////////////////////////////////////
	//	PrintWriter
	///////////////////////////////////////////////
	
	private PrintWriter	mPrintWriter	= null;				

	private void setPrintWriter(PrintWriter pw) {
		mPrintWriter = pw;
	}

	private PrintWriter getPrintWriter() {
		return mPrintWriter;
	}

	private void print(String msg) {
		getPrintWriter().print(msg);
	}
	
	private void println(String msg) {
		getPrintWriter().println(msg);
	}

	///////////////////////////////////////////////
	//	getShapeCount
	///////////////////////////////////////////////
	
	private int getShapeCount() {
		SceneGraph sg = getSceneGraph();
		int count = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					count++;
			}
		}
		return count;
	}

	///////////////////////////////////////////////
	//	getShapeVertexFormat
	///////////////////////////////////////////////

	private int getShapeVertexFormat(int nShape) {
		SceneGraph sg = getSceneGraph();
		int count = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					if (nShape == count) 
						return ((GeometryArray)nodeObject).getVertexFormat();
					count++;
				}
			}
		}
		return 0;
	}

	///////////////////////////////////////////////
	//	getShapeVertexCount
	///////////////////////////////////////////////

	private int getShapeVertexCount(int nShape) {
		SceneGraph sg = getSceneGraph();
		int count = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					if (nShape == count) 
						return ((GeometryArray)nodeObject).getVertexCount();
					count++;
				}
			}
		}
		return 0;
	}

	///////////////////////////////////////////////
	//	getUse*VertexCount
	///////////////////////////////////////////////

	private int getUseCoordinateVertexCount(GeometryArray geom) {
		if ((geom.getVertexFormat() & GeometryArray.COORDINATES) == 0)
			return 0;
			
		if ((geom instanceof IndexedGeometryArray) == false)
			return geom.getVertexCount();
		
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;
		
		int indexCount = idxGeom.getIndexCount();
		int maxIndex = 0;
		for (int n=0; n<indexCount; n++) {
			int index = idxGeom.getCoordinateIndex(n);
			if (maxIndex < index)
				maxIndex = index;
		}
		return (maxIndex + 1);
	}

	private int getUseColorVertexCount(GeometryArray geom) {
		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.COLOR_3) == 0 && (vertexFormat & GeometryArray.COLOR_4) == 0)
			return 0;
		
		if ((geom instanceof IndexedGeometryArray) == false)
			return geom.getVertexCount();
			
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;
		
		int indexCount = idxGeom.getIndexCount();
		int maxIndex = 0;
		for (int n=0; n<indexCount; n++) {
			int index = idxGeom.getColorIndex(n);
			if (maxIndex < index)
				maxIndex = index;
		}
		return (maxIndex + 1);
	}

	private int getUseNormalVertexCount(GeometryArray geom) {
		if ((geom.getVertexFormat() & GeometryArray.NORMALS) == 0)
			return 0;
		
		if ((geom instanceof IndexedGeometryArray) == false)
			return geom.getVertexCount();
		
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;
		
		int indexCount = idxGeom.getIndexCount();
		int maxIndex = 0;
		for (int n=0; n<indexCount; n++) {
			int index = idxGeom.getNormalIndex(n);
			if (maxIndex < index)
				maxIndex = index;
		}
		return (maxIndex + 1);
	}
	
	private int getUseTexCoordVertexCount(GeometryArray geom) {
		if ((geom.getVertexFormat() & GeometryArray.TEXTURE_COORDINATE_2) == 0)
			return 0;
		
		if ((geom instanceof IndexedGeometryArray) == false)
			return geom.getVertexCount();
		
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;
		
		int indexCount = idxGeom.getIndexCount();
		int maxIndex = 0;
		for (int n=0; n<indexCount; n++) {
			int index = idxGeom.getTextureCoordinateIndex(n);
			if (maxIndex < index)
				maxIndex = index;
		}
		return (maxIndex + 1);
	}

	///////////////////////////////////////////////
	//	isGeometryNode
	///////////////////////////////////////////////

	public boolean	isSupportedGeometryNode(Object geom) {
		if (geom instanceof IndexedGeometryArray)
			return true;
		if (geom instanceof TriangleArray)
			return true;
		if (geom instanceof QuadArray)
			return true;
		return false;
	}
	
	///////////////////////////////////////////////
	//	save (GeometryType)
	///////////////////////////////////////////////

	private void saveGeometryType(GeometryArray geom) {
		if (geom instanceof IndexedTriangleArray) {
			println("        GEOM_INDEXED_TRIANGLE_ARRAY,");
			return;
		}
		if (geom instanceof TriangleArray) {
			println("        GEOM_TRIANGLE_ARRAY,");
			return;
		}
		if (geom instanceof QuadArray) {
			println("        GEOM_QUAD_ARRAY,");
			return;
		}
		println("        GEOM_NONE,");
	}

	private void saveGeometryTypes() {
		println("    private final static int geomType[] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveGeometryType((GeometryArray)nodeObject);
			}
		}

		println("    };");
	}
	
	///////////////////////////////////////////////
	//	save (TransformMatrix)
	///////////////////////////////////////////////

	private Transform3D		trans3d			= new Transform3D();
	private Matrix4f			matrix			= new Matrix4f();
	private float				matrixRow[]		= new float[4];
	
	private Matrix4f getTransformMatrix(Node node) {
		Matrix4f transMatrix = new Matrix4f();
		transMatrix.setIdentity();
		
		Node parentNode = node.getParentNode();
		while (parentNode != null) {
			if (parentNode.isTransformNode() == true) {
				TransformGroup	transGroup = new TransformNodeObject((TransformNode)parentNode);
				transGroup.getTransform(trans3d);
				trans3d.get(matrix);
				transMatrix.mul(matrix);
			}
			parentNode = parentNode.getParentNode();
		}
		
		return transMatrix; 
	}
	
	private void saveTransformMatrix(Node node) {
		Matrix4f transMatrix = getTransformMatrix(node);
		println("        {");
		for (int n=0; n<4; n++) {
			transMatrix.getRow(n, matrixRow);
		println("            {" + matrixRow[0] + "f, " + matrixRow[1] + "f, " + matrixRow[2] + "f, " + matrixRow[3] + "f},");
		}
		println("        },");
	}

	private void saveTransformMatrices() {
		println("    private final static float transformMatrix[][][] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveTransformMatrix(node);
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (Material)
	///////////////////////////////////////////////

	private MaterialNode	defaultMaterialNode	= new MaterialNode();
	private	float				color[]						= new float[3];
	
	private MaterialNode getMaterialNode(Node node) {
		Node parentNode = node.getParentNode();
		if (parentNode.isShapeNode() == false)
			return defaultMaterialNode;
		ShapeNode shapeNode = (ShapeNode)parentNode;
		AppearanceNode appNode = shapeNode.getAppearanceNodes();
		if (appNode == null)
			return defaultMaterialNode;
		MaterialNode matNode = appNode.getMaterialNodes();
		if (matNode == null)		
			return defaultMaterialNode;
		return matNode;
	}
	
	private void saveMaterial(MaterialNode matNode) {
		println("        {");
		
		matNode.getAmbientColor(color);
		println("            {" + color[0] + "f, " + color[1] + "f, " + color[2] + "f},");
		
		matNode.getDiffuseColor(color);
		println("            {" + color[0] + "f, " + color[1] + "f, " + color[2] + "f},");
		
		matNode.getEmissiveColor(color);
		println("            {" + color[0] + "f, " + color[1] + "f, " + color[2] + "f},");
		
		matNode.getSpecularColor(color);
		println("            {" + color[0] + "f, " + color[1] + "f, " + color[2] + "f},");
		
		println("            {" + matNode.getShininess() + "f},");
		
		println("        },");
	}

	private void saveMaterials() {
		println("    private final static float material[][][] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveMaterial(getMaterialNode(node));
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (Texture)
	///////////////////////////////////////////////

	private BufferedImage getTexture2DBufferedImage(Node node) {
		Node parentNode = node.getParentNode();
		if (parentNode.isShapeNode() == false)
			return null;
		ShapeNode shapeNode = (ShapeNode)parentNode;
		AppearanceNode appNode = shapeNode.getAppearanceNodes();
		if (appNode == null)
			return null;
		TextureNode texNode = appNode.getTextureNode();
		if (texNode == null)		
			return null;
		NodeObject texNodeObject = texNode.getObject();
		if (texNodeObject == null)		
			return null;
		if ((texNodeObject instanceof Texture2D) == false)		
			return null;
		Texture2D texture = (Texture2D)texNodeObject;
		
		ImageComponent imgComp = texture.getImage(0);

		if (imgComp == null)
			return null;

		if ((imgComp instanceof ImageComponent2D) == false) 
			return null;

		ImageComponent2D imgComp2D = (ImageComponent2D)imgComp;
		
		return imgComp2D.getImage();
	}

	private int getTargaFormatType(int textureMode) {
		if (textureMode == TEXTURE_TARGA_RGB24) 
			return Targa.FORMAT_RGB_24;
		if (textureMode == TEXTURE_TARGA_RGBA32) 
			return Targa.FORMAT_RGB_32;
		if (textureMode == TEXTURE_TARGA_RGBA16) 
			return Targa.FORMAT_RGB_16;
		return -1;
	}

	private void saveTexture(int nGeom, BufferedImage bufImage, int textureMode) {
		if (bufImage == null || textureMode == TEXTURE_NONE) {
			println("        null,");
			return;
		}
		
		Targa targa = new Targa();
		targa.setBufferedImage(bufImage);
		
		int formatType = getTargaFormatType(textureMode);
		if (formatType < 0) {
			println("        null,");
			return;
		}
		
		String fileName = "texture" + nGeom + ".tga";

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
				
		if (targa.save(compOutStream, formatType) == true)
			println("        \"" + fileName + "\",");
		else
			println("        null,");
	}

	private void saveTextures(int textureMode) {
		println("    private final static String texture[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveTexture(nGeom, getTexture2DBufferedImage(node), textureMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (VertexCount)
	///////////////////////////////////////////////

	private void saveVertexCount(GeometryArray idxGeom) {
		int vertexCount = idxGeom.getVertexCount();
		println("        " + vertexCount + ",");
	}

	private void saveVertexCounts() {
		println("    private final static int vertexCount[] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveVertexCount((GeometryArray)nodeObject);
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (VertexFormat)
	///////////////////////////////////////////////

	private void saveVertexFormat(GeometryArray idxGeom, int colorMode) {
		int vertexCount = idxGeom.getVertexCount();
		int vertexFormat = idxGeom.getVertexFormat();
		StringBuffer formatString = new StringBuffer("(GeometryArray.COORDINATES");
		if ((vertexFormat & GeometryArray.NORMALS) != 0)
			formatString.append(" | GeometryArray.NORMALS");
		if (colorMode == COLOR_RGB24) {
			if ((vertexFormat & GeometryArray.COLOR_3) != 0)
				formatString.append(" | GeometryArray.COLOR_3");
		}
		else if (colorMode == COLOR_RGBA32 || colorMode == COLOR_RGBA16) {
			if ((vertexFormat & GeometryArray.COLOR_4) != 0)
				formatString.append(" | GeometryArray.COLOR_4");
		}
		if ((vertexFormat & GeometryArray.TEXTURE_COORDINATE_2) != 0)
			formatString.append(" | GeometryArray.TEXTURE_COORDINATE_2");
		formatString.append(")");
		println("        " + formatString.toString() + ",");
	}

	private void saveVertexFormats(int colorMode) {
		println("    private final static int vertexFormat[] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveVertexFormat((GeometryArray)nodeObject, colorMode);
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	toARGBColor
	///////////////////////////////////////////////
	
	private byte to8bitColor(float color) {
		if (color < 0.0)
			color = 0.0f;
		if (1.0 < color)
			color = 1.0f; 
		return (byte)(color * 255.0f); 
	}

	private byte to5bitColor(float color) {
		if (color < 0.0)
			color = 0.0f;
		if (1.0 < color)
			color = 1.0f; 
		return (byte)(color * 31.0f); 
	}
	
	private int toRGBA32bitColor(float color[]) {
		int color32 = 0;
		color32 |= ((int)to8bitColor(color[0]) << 16) & 0x00FF0000;
		color32 |= ((int)to8bitColor(color[1]) <<  8) & 0x0000FF00;
		color32 |= ((int)to8bitColor(color[2]) <<  0) & 0x000000FF;
		if (3 < color.length)
			color32 |= ((int)to8bitColor(color[3]) << 24) & 0xFF000000;
		return color32;
	}

	private short toRGBA16bitColor(float color[]) {
		short color16 = 0;
		color16 |= ((short)to5bitColor(color[0]) << 10) & 0x7C00;
		color16 |= ((short)to5bitColor(color[1]) <<  5) & 0x03E0;
		color16 |= ((short)to5bitColor(color[2]) <<  0) & 0x001F;
		if (3 < color.length) {
			if (color[3] != 0.0)
				color16 |= 0x8000;
		}
		return color16;
	}

	///////////////////////////////////////////////
	//
	//	Geometry Infomation
	//
	///////////////////////////////////////////////
	
	private void getCoordinateExtents(GeometryArray geom, float extents[][]) {
		for (int n=0; n<3; n++) {
			extents[n][MIN] = Float.MAX_VALUE;
			extents[n][MAX] = Float.MIN_VALUE;
		}
		
		int vertexCount = getUseCoordinateVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getCoordinate(n, coord);
			for (int i=0; i<3; i++) {
				if (coord[i] < extents[i][MIN])
					extents[i][MIN] = coord[i];
				if (extents[i][MAX] < coord[i])
					extents[i][MAX] = coord[i];
			}
		}
	}

	private void getNormalExtents(GeometryArray geom, float extents[][]) {
		for (int n=0; n<3; n++) {
			extents[n][MIN] = Float.MAX_VALUE;
			extents[n][MAX] = Float.MIN_VALUE;
		}
		
		int vertexCount = getUseNormalVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getNormal(n, coord);
			for (int i=0; i<3; i++) {
				if (coord[i] < extents[i][MIN])
					extents[i][MIN] = coord[i];
				if (extents[i][MAX] < coord[i])
					extents[i][MAX] = coord[i];
			}
		}
	}

	private void getTexCoordExtents(GeometryArray geom, float extents[][]) {
		for (int n=0; n<2; n++) {
			extents[n][MIN] = Float.MAX_VALUE;
			extents[n][MAX] = Float.MIN_VALUE;
		}
		
		int vertexCount = getUseTexCoordVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getTextureCoordinate(n, coord);
			for (int i=0; i<2; i++) {
				if (coord[i] < extents[i][MIN])
					extents[i][MIN] = coord[i];
				if (extents[i][MAX] < coord[i])
					extents[i][MAX] = coord[i];
			}
		}
	}

	///////////////////////////////////////////////
	//
	//	Precision
	//
	///////////////////////////////////////////////

	private float getCoordinatePrecision(int coordMode) {
		float precision = 0.0f;
		switch (coordMode) {
		case COORD_24:
			precision = (float)0x00FFFFFF;
			break;
		case COORD_16:
			precision = (float)0x0000FFFF;
			break;
		}
		return precision;
	}

	private float getNormalPrecision(int normalMode) {
		float precision = 0.0f;
		switch (normalMode) {
		case NORMAL_24:
			precision = (float)0x00FFFFFF;
			break;
		case NORMAL_16:
			precision = (float)0x0000FFFF;
			break;
		case NORMAL_8:
			precision = (float)0x000000FF;
			break;
		}
		return precision;
	}

	private float getTexCoordPrecision(int texCoordMode) {
		float precision = 0.0f;
		switch (texCoordMode) {
		case TEXCOORD_24:
			precision = (float)0x00FFFFFF;
			break;
		case TEXCOORD_16:
			precision = (float)0x0000FFFF;
			break;
		case TEXCOORD_8:
			precision = (float)0x000000FF;
			break;
		}
		return precision;
	}

	private void getExtentsPrecision(float extents[][], float precision, float extentsPrecision[], int arraySize) {
		for (int n=0; n<arraySize; n++)
			extentsPrecision[n] = (extents[n][MAX] - extents[n][MIN]) / precision;
	}

	private void getExtentsRange(float extents[][], float extentsRange[], int arraySize) {
		for (int n=0; n<arraySize; n++)
			extentsRange[n] = extents[n][MAX] - extents[n][MIN];
	}
	
	///////////////////////////////////////////////
	//
	//	CoordinateRange Save Methods
	//
	///////////////////////////////////////////////

	private float extents[][]			= new float[3][2];
	private float extentsPrecision[]	= new float[3];
	private float extentsRange[]		= new float[3];
	private float precision;
		
	private void saveCoordinatePrecision(GeometryArray geom, int nGeom, int coordMode) {
		String fileName = "coordPrecision" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
		
		float precision = getCoordinatePrecision(coordMode);
		getCoordinateExtents(geom, extents);
		getExtentsPrecision(extents, precision, extentsPrecision, 3);
		getExtentsRange(extents, extentsRange, 3);
		
		for (int n=0; n<3; n++) {
			writeFloat(compOutStream, extents[n][MIN]);
			writeFloat(compOutStream, extentsRange[n]);	
		}
	}

	private void saveCoordinatePrecisions(int coordMode) {
		println("    private static float coordPrecision[][][] = null;");
		println("    private final static String coordPrecisionName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveCoordinatePrecision((GeometryArray)nodeObject, nGeom, coordMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	NormalRange Save Methods
	//
	///////////////////////////////////////////////

	private void saveNormalPrecision(GeometryArray geom, int nGeom, int coordMode) {
		String fileName = "normalPrecision" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
		
		float precision = getNormalPrecision(coordMode);
		getNormalExtents(geom, extents);
		getExtentsPrecision(extents, precision, extentsPrecision, 3);
		getExtentsRange(extents, extentsRange, 3);
		
		for (int n=0; n<3; n++) {
			writeFloat(compOutStream, extents[n][MIN]);
			writeFloat(compOutStream, extentsRange[n]);	
		}
	}

	private void saveNormalPrecisions(int coordMode) {
		println("    private static float normalPrecision[][][] = null;");
		println("    private final static String normalPrecisionName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveNormalPrecision((GeometryArray)nodeObject, nGeom, coordMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	TexCoordRange Save Methods
	//
	///////////////////////////////////////////////

	private void saveTexCoordPrecision(GeometryArray geom, int nGeom, int coordMode) {
		String fileName = "texCoordPrecision" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
		
		float precision = getTexCoordPrecision(coordMode);
		getTexCoordExtents(geom, extents);
		getExtentsPrecision(extents, precision, extentsPrecision, 2);
		getExtentsRange(extents, extentsRange, 2);
		
		for (int n=0; n<2; n++) {
			writeFloat(compOutStream, extents[n][MIN]);
			writeFloat(compOutStream, extentsRange[n]);	
		}
	}

	private void saveTexCoordPrecisions(int coordMode) {
		println("    private static float texCoordPrecision[][][] = null;");
		println("    private final static String texCoordPrecisionName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveTexCoordPrecision((GeometryArray)nodeObject, nGeom, coordMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	Coordinate Save Methods
	//
	///////////////////////////////////////////////

	private float	coord[] = new float[3];
	
	private void saveCoordinate(GeometryArray geom, int nGeom, int coordMode) {
		String fileName = "coord" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		if (coordMode != COORD_32) {
			precision = getCoordinatePrecision(coordMode);
			getCoordinateExtents(geom, extents);
			getExtentsPrecision(extents, precision, extentsPrecision, 3);
			getExtentsRange(extents, extentsRange, 3);
		}
		
		int vertexCount = getUseCoordinateVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getCoordinate(n, coord);
			switch (coordMode) {
			case COORD_32:
				writeFloat(compOutStream, coord[0]);	
				writeFloat(compOutStream, coord[1]);	
				writeFloat(compOutStream, coord[2]);	
				break;
			case COORD_24:
				for (int i=0; i<3; i++) {
					int offset = (int)((coord[i] - extents[i][MIN]) / extentsRange[i] * precision); 
					writeInteger24(compOutStream, offset);
				}
				break;
			case COORD_16: 
				for (int i=0; i<3; i++) {
					int offset = (int)((coord[i] - extents[i][MIN]) / extentsRange[i]* precision); 
					writeInteger16(compOutStream, offset);
				}
				break;
			}
		}
	}

	private void saveCoordinates(int coordMode) {
		println("    private static float coord[][][] = null;");
		println("    private final static String coordName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveCoordinate((GeometryArray)nodeObject, nGeom, coordMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	Color Save Methods
	//
	///////////////////////////////////////////////

	//private float color[] = new float[3];
	
	private void saveColor(GeometryArray geom, int nGeom, int colorMode) {
		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.COLOR_3) == 0 && (vertexFormat & GeometryArray.COLOR_4) == 0) {
		println("        null,");
			return;
		}

		String fileName = "color" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
		int vertexCount = getUseColorVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getColor(n, color);			
			if (colorMode == COLOR_RGB24)
				writeRGB24Color(compOutStream, toRGBA32bitColor(color));	
			else if (colorMode == COLOR_RGBA32)
				writeRGB32Color(compOutStream, toRGBA32bitColor(color));	
			else  //if (colorMode == COLOR_RGBA16)
				writeRGB16Color(compOutStream, toRGBA16bitColor(color));	
		}
	}

	private void saveColors(int colorMode) {
		println("    private static float color[][][] = null;");
		if (colorMode == COLOR_RGB24)
			println("    private final static String color24Name[] = {");
		else if (colorMode == COLOR_RGBA32)
			println("    private final static String color32Name[] = {");
		else //if (colorMode == COLOR_RGBA16)
			println("    private final static String color16Name[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveColor((GeometryArray)nodeObject, nGeom, colorMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	Normal Save Methods
	//
	///////////////////////////////////////////////

	private float normal[] = new float[3];

	private void saveNormal(GeometryArray geom, int nGeom, int normalMode) {
		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.NORMALS) == 0) {
		println("        null,");
			return;
		}

		String fileName = "normal" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		if (normalMode != NORMAL_32) {
			precision = getNormalPrecision(normalMode);
			getNormalExtents(geom, extents);
			getExtentsPrecision(extents, precision, extentsPrecision, 3);
			getExtentsRange(extents, extentsRange, 3);
		}

		int vertexCount = getUseNormalVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getNormal(n, normal);			
			switch (normalMode) {
			case NORMAL_32:
				writeFloat(compOutStream, normal[0]);	
				writeFloat(compOutStream, normal[1]);	
				writeFloat(compOutStream, normal[2]);	
				break;
			case NORMAL_24:
				for (int i=0; i<3; i++) {
					int offset = (int)((normal[i] - extents[i][MIN]) / extentsRange[i] * precision); 
					writeInteger24(compOutStream, offset);
				}
				break;
			case NORMAL_16: 
				for (int i=0; i<3; i++) {
					int offset = (int)((normal[i] - extents[i][MIN]) / extentsRange[i]* precision); 
					writeInteger16(compOutStream, offset);
				}
				break;
			case NORMAL_8: 
				for (int i=0; i<3; i++) {
					int offset = (int)((normal[i] - extents[i][MIN]) / extentsRange[i]* precision); 
					writeInteger8(compOutStream, offset);
				}
				break;
			}
		}
	}

	private void saveNormals(int normalMode) {
		println("    private static float normal[][][] = null;");
		println("    private final static String normalName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveNormal((GeometryArray)nodeObject, nGeom, normalMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//
	//	TextureCoordinate Save Methods
	//
	///////////////////////////////////////////////

	private float texCoord[] = new float[2];

	private void saveTextureCoordinate(GeometryArray geom, int nGeom, int texCoordMode) {
		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.TEXTURE_COORDINATE_2) == 0) {
		println("        null,");
			return;
		}

		String fileName = "texCoord" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		if (texCoordMode != TEXCOORD_32) {
			precision = getTexCoordPrecision(texCoordMode);
			getTexCoordExtents(geom, extents);
			getExtentsPrecision(extents, precision, extentsPrecision, 2);
			getExtentsRange(extents, extentsRange, 2);
		}
		
		int vertexCount = getUseTexCoordVertexCount(geom); //geom.getVertexCount();
		for (int n=0; n<vertexCount; n++) {
			geom.getTextureCoordinate(n, texCoord);			
			switch (texCoordMode) {
			case TEXCOORD_32:
				writeFloat(compOutStream, texCoord[0]);	
				writeFloat(compOutStream, texCoord[1]);	
				break;
			case TEXCOORD_24:
				for (int i=0; i<2; i++) {
					int offset = (int)((texCoord[i] - extents[i][MIN]) / extentsRange[i] * precision); 
					writeInteger24(compOutStream, offset);
				}
				break;
			case TEXCOORD_16: 
				for (int i=0; i<2; i++) {
					int offset = (int)((texCoord[i] - extents[i][MIN]) / extentsRange[i]* precision); 
					writeInteger16(compOutStream, offset);
				}
				break;
			case TEXCOORD_8: 
				for (int i=0; i<2; i++) {
					int offset = (int)((texCoord[i] - extents[i][MIN]) / extentsRange[i]* precision); 
					writeInteger8(compOutStream, offset);
				}
				break;
			}

		}
	}

	private void saveTextureCoordinates(int texCoordMode) {
		println("    private static float texCoord[][][] = null;");
		println("    private final static String texCoordName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveTextureCoordinate((GeometryArray)nodeObject, nGeom, texCoordMode);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (IndexCount)
	///////////////////////////////////////////////

	private void saveIndexCount(GeometryArray geom) {
		if ((geom instanceof IndexedGeometryArray) == false) {
		println("        0,");
			return;
		}
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;
		int indexCount = idxGeom.getIndexCount();
		println("        " + indexCount + ",");
	}

	private void saveIndexCounts() {
		println("    private final static int indexCount[] = {");
		
		SceneGraph sg = getSceneGraph();
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true)
					saveIndexCount((GeometryArray)nodeObject);
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (CoordinateIndex)
	///////////////////////////////////////////////

	private void saveCoordinateIndex(GeometryArray geom, int nGeom) {
		if ((geom instanceof IndexedGeometryArray) == false) {
		println("        null,");
			return;
		}
		
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;

		String fileName = "coordIndex" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		int indexCount = idxGeom.getIndexCount();
		int indexMode = INDEX_32;
		if (indexCount < (int)0xFFFF)
			indexMode = INDEX_16;
		if (indexCount < (int)0x00FF)
			indexMode = INDEX_8;
			
		for (int n=0; n<indexCount; n++) {
			int id = idxGeom.getCoordinateIndex(n);
			if (indexMode == INDEX_32)
				writeInteger(compOutStream, id);	
			else if (indexMode == INDEX_16)
				writeShort(compOutStream, (short)id);	
			else if (indexMode == INDEX_8)
				writeByte(compOutStream, (byte)id);	
		}
	}

	private void saveCoordinateIndices() {
		println("    private static int coordIndex[][] = null;");
		println("    private final static String coordIndexName[] = {");
			
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveCoordinateIndex((GeometryArray)nodeObject, nGeom);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	save (ColorIndex)
	///////////////////////////////////////////////

	private void saveColorIndex(GeometryArray geom, int nGeom) {
		if ((geom instanceof IndexedGeometryArray) == false) {
		println("        null,");
			return;
		}

		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.COLOR_3) == 0 && (vertexFormat & GeometryArray.COLOR_4) == 0 ){ 
		println("        null,");
			return;
		}
				
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;

		String fileName = "colorIndex" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);
	
		int indexCount = idxGeom.getIndexCount();
		int indexMode = INDEX_32;
		if (indexCount < (int)0xFFFF)
			indexMode = INDEX_16;
		if (indexCount < (int)0x00FF)
			indexMode = INDEX_8;
			
		for (int n=0; n<indexCount; n++) {
			int id = idxGeom.getColorIndex(n);
			if (indexMode == INDEX_32)
				writeInteger(compOutStream, id);	
			else if (indexMode == INDEX_16)
				writeShort(compOutStream, (short)id);	
			else if (indexMode == INDEX_8)
				writeByte(compOutStream, (byte)id);	
		}
	}

	private void saveColorIndices() {
		println("    private static int colorIndex[][] = null;");
		println("    private final static String colorIndexName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveColorIndex((GeometryArray)nodeObject, nGeom);
					nGeom++;
				}
			}
		}

		println("    };");
	}
	
	///////////////////////////////////////////////
	//	save (NormalIndex)
	///////////////////////////////////////////////

	private void saveNormalIndex(GeometryArray geom, int nGeom) {
		if ((geom instanceof IndexedGeometryArray) == false) {
		println("        null,");
			return;
		}

		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.NORMALS) == 0){ 
		println("        null,");
			return;
		}
				
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;

		String fileName = "normalIndex" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		int indexCount = idxGeom.getIndexCount();
		int indexMode = INDEX_32;
		if (indexCount < (int)0xFFFF)
			indexMode = INDEX_16;
		if (indexCount < (int)0x00FF)
			indexMode = INDEX_8;
			
		for (int n=0; n<indexCount; n++) {
			int id = idxGeom.getNormalIndex(n);
			if (indexMode == INDEX_32)
				writeInteger(compOutStream, id);	
			else if (indexMode == INDEX_16)
				writeShort(compOutStream, (short)id);	
			else if (indexMode == INDEX_8)
				writeByte(compOutStream, (byte)id);	
		}
	}

	private void saveNormalIndices() {
		println("    private static int normalIndex[][] = null;");
		println("    private final static String normalIndexName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveNormalIndex((GeometryArray)nodeObject, nGeom);
					nGeom++;
				}
			}
		}

		println("    };");
	}
	
	///////////////////////////////////////////////
	//	save (TextureCoordinateIndex)
	///////////////////////////////////////////////

	private void saveTextureCoordinateIndex(GeometryArray geom, int nGeom) {
		if ((geom instanceof IndexedGeometryArray) == false) {
		println("        null,");
			return;
		}

		int vertexFormat = geom.getVertexFormat();
		if ((vertexFormat & GeometryArray.TEXTURE_COORDINATE_2) == 0){ 
		println("        null,");
			return;
		}
				
		IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;

		String fileName = "texCoordIndex" + nGeom + ".bin";
		println("        \"" + fileName + "\",");

		JarOutputStream compOutStream = openNewCompressionOutputStream(fileName);

		int indexCount = idxGeom.getIndexCount();
		int indexMode = INDEX_32;
		if (indexCount < (int)0xFFFF)
			indexMode = INDEX_16;
		if (indexCount < (int)0x00FF)
			indexMode = INDEX_8;
			
		for (int n=0; n<indexCount; n++) {
			int id = idxGeom.getTextureCoordinateIndex(n);
			if (indexMode == INDEX_32)
				writeInteger(compOutStream, id);	
			else if (indexMode == INDEX_16)
				writeShort(compOutStream, (short)id);	
			else if (indexMode == INDEX_8)
				writeByte(compOutStream, (byte)id);	
		}
	}

	private void saveTextureCoordinateIndices() {
		println("    private static int texCoordIndex[][] = null;");
		println("    private final static String texCoordIndexName[] = {");
		
		SceneGraph sg = getSceneGraph();
		int nGeom = 0;
		for (Node node=sg.getNodes(); node != null; node=node.nextTraversal()) {
			NodeObject nodeObject = node.getObject();
			if (nodeObject != null) {
				if (isSupportedGeometryNode(nodeObject) == true) {
					saveTextureCoordinateIndex((GeometryArray)nodeObject, nGeom);
					nGeom++;
				}
			}
		}

		println("    };");
	}

	///////////////////////////////////////////////
	//	Precision Methods 
	///////////////////////////////////////////////

	private void savePrecisionMethods() {
		println("    public float[][]createPrecisions(String filename) {");
		println("        if (filename == null)");
		println("            return null;");
		println("        JarInputStream in = getCompressionInputStream(filename);");
		println("        if (in == null)");
		println("            return null;");
		println("        float precision[][] = new float[3][2];");
		println("        for (int n=0; n<3; n++) {");
		println("            precision[n][0] = readFloat(in);");
		println("            precision[n][1] = readFloat(in);");
		println("        }");
		println("        closeCompressionInputStream(in);");
		println("        return precision;");
		println("    }");
	}

	private void saveSetPrecisionData(int coordMode, int normalMode, int colorMode, int texCoordMode) {
		if (coordMode != COORD_32) 
		println("        coordPrecision  = new float[shapeCount][0][0];");
		
		if (normalMode != NORMAL_32) 
		println("        normalPrecision  = new float[shapeCount][0][0];");
		
		if (texCoordMode != TEXCOORD_32) 
		println("        texCoordPrecision  = new float[shapeCount][0][0];");

		println("        for (int n=0; n<shapeCount; n++) {");
		if (coordMode != COORD_32) 
		println("            coordPrecision[n] = createPrecisions(coordPrecisionName[n]);");
		if (normalMode != NORMAL_32) 
		println("            normalPrecision[n] = createPrecisions(normalPrecisionName[n]);");
		if (texCoordMode != TEXCOORD_32) 
		println("            texCoordPrecision[n] = createPrecisions(texCoordPrecisionName[n]);");
		println("        }");
	}
	
	///////////////////////////////////////////////
	//	Decompress Methods 
	///////////////////////////////////////////////

	private void saveSetDecompressionDataMethods(int coordMode, int normalMode, int colorMode, int texCoordMode) {
		println("    public float[][] createFloatPoints(String filename, int vertexCount, int valueCount, int precisionBit, float precision[][]) {");
		println("        if (filename == null)");
		println("            return null;");
		println("        JarInputStream in = getCompressionInputStream(filename);");
		println("        if (in == null)");
		println("            return null;");
		println("        float point[][] = new float[vertexCount][3];");
		println("        for (int i=0; i<vertexCount; i++) {");
		println("            for (int j=0; j<valueCount; j++) {");
		println("                switch (precisionBit) {");
		println("                case 32:");
		println("                    point[i][j] = readFloat(in);");
		println("                    break;");
		println("                case 24:");
		println("                    point[i][j] = ((float)readInteger24(in) / (float)0x00FFFFFF) * precision[j][1] + precision[j][0];");
		println("                    break;");
		println("                case 16:");
		println("                    point[i][j] = ((float)readInteger16(in) / (float)0x0000FFFF) * precision[j][1] + precision[j][0];");
		println("                    break;");
		println("                case  8:");
		println("                    point[i][j] = ((float)readInteger8(in)  / (float)0x000000FF) * precision[j][1] + precision[j][0];");
		println("                    break;");
		println("                }");
		println("            }");
		println("        }");
		println("        closeCompressionInputStream(in);");
		println("        return point;");
		println("    }");

		println("    public float[][] createCoordinatePoints(int n) {");
		switch (coordMode) {
		case COORD_32:
			println("        return createFloatPoints(coordName[n], vertexCount[n], 3, 32, null);");
			break;
		case COORD_24:
			println("        return createFloatPoints(coordName[n], vertexCount[n], 3, 24, coordPrecision[n]);");
			break;
		case COORD_16:
			println("        return createFloatPoints(coordName[n], vertexCount[n], 3, 16, coordPrecision[n]);");
			break;
		}
		println("    }");
		

		println("    public float[][] createColors(int n) {");
		
		if (colorMode == COLOR_RGB24)
		println("        String filename = color24Name[n];");
		else if (colorMode == COLOR_RGBA32)
		println("        String filename = color32Name[n];");
		else //if (colorMode == COLOR_RGBA16)
		println("        String filename = color16Name[n];");
		
		println("        if (filename == null)");
		println("            return null;");
		println("        JarInputStream in = getCompressionInputStream(filename);");
		println("        if (in == null)");
		println("            return null;");
		println("        int colorCount = vertexCount[n];");
		
		if (colorMode == COLOR_RGB24)
		println("        float color[][] = new float[colorCount][3];");
		else //if (colorMode == COLOR_RGBA32 || colorMode == COLOR_RGBA16)
		println("        float color[][] = new float[colorCount][4];");
		
		println("        for (int i=0; i<colorCount; i++)");
		
		if (colorMode == COLOR_RGB24)
		println("            readColor24(in, color[i]);");
		else if (colorMode == COLOR_RGBA32)
		println("            readColor32(in, color[i]);");
		else //if (colorMode == COLOR_RGBA16)
		println("            readColor16(in, color[i]);");
		
		println("        closeCompressionInputStream(in);");
		println("        return color;");
		println("    }");
		
		println("    public float[][] createNormalVectors(int n) {");
		switch (normalMode) {
		case NORMAL_32:
			println("        return createFloatPoints(normalName[n], vertexCount[n], 3, 32, null);");
			break;
		case NORMAL_24:
			println("        return createFloatPoints(normalName[n], vertexCount[n], 3, 24, normalPrecision[n]);");
			break;
		case NORMAL_16:
			println("        return createFloatPoints(normalName[n], vertexCount[n], 3, 16, normalPrecision[n]);");
			break;
		case NORMAL_8:
			println("        return createFloatPoints(normalName[n], vertexCount[n], 3, 8, normalPrecision[n]);");
			break;
		}
		println("    }");
		
		println("    public float[][] createTextureCoordinatePoints(int n) {");
		switch (texCoordMode) {
		case TEXCOORD_32:
			println("        return createFloatPoints(texCoordName[n], vertexCount[n], 2, 32, null);");
			break;
		case TEXCOORD_24:
			println("        return createFloatPoints(texCoordName[n], vertexCount[n], 2, 24, texCoordPrecision[n]);");
			break;
		case TEXCOORD_16:
			println("        return createFloatPoints(texCoordName[n], vertexCount[n], 2, 16, texCoordPrecision[n]);");
			break;
		case TEXCOORD_8:
			println("        return createFloatPoints(texCoordName[n], vertexCount[n], 2, 8, texCoordPrecision[n]);");
			break;
		}
		println("    }");

		println("    public int[] createIndices(String filename, int indexCount) {");
		println("        if (filename == null)");
		println("            return null;");
		println("        JarInputStream in = getCompressionInputStream(filename);");
		println("        if (in == null)");
		println("            return null;");
		println("        int bit = 32;");
		println("        if (indexCount < (int)0xFFFF)");
		println("            bit = 16;");
		println("        if (indexCount < (int)0x00FF)");
		println("            bit = 8;");
		println("        int index[] = new int[indexCount];");
		println("        for (int i=0; i<indexCount; i++) {");
		println("            switch (bit) {");
		println("            case 32:");
		println("                index[i] = readInteger(in);");
		println("                break;");
		println("            case 16:");
		println("                index[i] = (int)readShort(in);");
		println("                break;");
		println("            case 8:");
		println("                index[i] = (int)readByte(in);");
		println("                break;");
		println("            }");
		println("        }");
		
		println("        closeCompressionInputStream(in);");
		println("        return index;");
		println("    }");
		println("    public int[] createCoordinateIndices(int n) {");
		println("        return createIndices(coordIndexName[n], indexCount[n]);");
		println("    }");
		println("    public int[] createColorIndices(int n) {");
		println("        return createIndices(colorIndexName[n], indexCount[n]);");
		println("    }");

		println("    public int[] createNormalIndices(int n) {");
		println("        return createIndices(normalIndexName[n], indexCount[n]);");
		println("    }");

		println("    public int[] createTextureCoordinateIndices(int n) {");
		println("        return createIndices(texCoordIndexName[n], indexCount[n]);");
		println("    }");
	}
	
	private void saveSetDecompressionData() {
		println("        coord         = new float[shapeCount][0][0];");
		println("        color         = new float[shapeCount][0][0];");
		println("        normal        = new float[shapeCount][0][0];");
		println("        texCoord      = new float[shapeCount][0][0];");
		println("        coordIndex    = new int[shapeCount][0];");
		println("        colorIndex    = new int[shapeCount][0];");
		println("        normalIndex   = new int[shapeCount][0];");
		println("        texCoordIndex = new int[shapeCount][0];");
		
		println("        for (int n=0; n<shapeCount; n++) {");
		
		println("            coord[n] = createCoordinatePoints(n);");

		println("            if ((vertexFormat[n] & GeometryArray.COLOR_3) != 0 || (vertexFormat[n] & GeometryArray.COLOR_4) != 0)");
		println("                color[n] = createColors(n);");
		
		println("            if ((vertexFormat[n] & GeometryArray.NORMALS) != 0)");
		println("                normal[n] = createNormalVectors(n);");

		println("            if ((vertexFormat[n] & GeometryArray.TEXTURE_COORDINATE_2) != 0)");
		println("                texCoord[n] = createTextureCoordinatePoints(n);");
		
		println("            coordIndex[n] = createCoordinateIndices(n);");

		println("            if ((vertexFormat[n] & GeometryArray.COLOR_3) != 0 || (vertexFormat[n] & GeometryArray.COLOR_4) != 0)");
		println("                colorIndex[n] = createColorIndices(n);");

		println("            if ((vertexFormat[n] & GeometryArray.NORMALS) != 0)");
		println("                normalIndex[n] = createNormalIndices(n);");

		println("            if ((vertexFormat[n] & GeometryArray.TEXTURE_COORDINATE_2) != 0)");
		println("                texCoordIndex[n] = createTextureCoordinateIndices(n);");

		println("        }");
	}

	///////////////////////////////////////////////
	//	save Constructor
	///////////////////////////////////////////////

	private void saveConstructor(int coordMode, int normalMode, int colorMode, int textureMode, int texCoordMode) {
		int shapeCount = getShapeCount();	
		println("    private TransformGroup mShapeTransGroup[];");
		println("    private URL docBase = null;");
		println("    public " + getClassName() + "(URL docBase) {");
		println("        this.docBase = docBase;");
		println("        int shapeCount = getShapeCount();");
		println("        mShapeTransGroup = new TransformGroup[shapeCount];");

		if (coordMode != COORD_32 || normalMode != NORMAL_32 || texCoordMode != TEXCOORD_32)
			saveSetPrecisionData(coordMode, normalMode, colorMode, texCoordMode);
			
		saveSetDecompressionData();
			
		println("    }");

		println("    public " + getClassName() + "() {");
		println("        this(null);");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save getShape3Dcount()
	///////////////////////////////////////////////

	private void saveGetShapeCountMethod() {
		println("    public int getShapeCount() {");
		println("        return " + getShapeCount() + ";");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save createTransformGroup()
	///////////////////////////////////////////////

	private void saveCreateTransformGroupMethod() {
		println("    private TransformGroup createTransformGroup(int n) {");
		println("        TransformGroup transGroup = new TransformGroup();");
		println("        Matrix4f matrix = new Matrix4f();");
		println("        for (int i=0; i<4; i++)");
		println("            matrix.setRow(i, transformMatrix[n][i]);");
		println("        Transform3D trans3D = new Transform3D();");
		println("        transGroup.getTransform(trans3D);");
		println("        trans3D.set(matrix);");
		println("        transGroup.setTransform(trans3D);");
		println("        return transGroup;");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save createMaterial()
	///////////////////////////////////////////////

	private void saveCreateMaterialMethod() {
		println("    private Material createMaterial(int n) {");
		println("        Material mat = new Material();");
		println("        mat.setAmbientColor (material[n][0][0], material[n][0][1], material[n][0][2]);");
		println("        mat.setDiffuseColor (material[n][1][0], material[n][1][1], material[n][1][2]);");
		println("        mat.setEmissiveColor(material[n][2][0], material[n][2][1], material[n][2][2]);");
		println("        mat.setSpecularColor(material[n][3][0], material[n][3][1], material[n][3][2]);");
		println("        mat.setShininess(material[n][4][0]);");
		println("        return mat;");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save Targa Methods
	///////////////////////////////////////////////

	private int getTexturePixelSize(int textureMode) {
    	int pixelSize = 0;
    	if (textureMode == TEXTURE_TARGA_RGB24)
    		pixelSize = 24;
    	if (textureMode == TEXTURE_TARGA_RGBA16)
    		pixelSize = 16;
    	if (textureMode == TEXTURE_TARGA_RGBA32)
    		pixelSize = 32;
    	return pixelSize;
	}
		
	private void saveLoadTextureMethod(int textureMode) {
    	int pixelSize = getTexturePixelSize(textureMode);
		println("    private BufferedImage loadTexture(InputStream in) {");
		println("        BufferedImage bufImage = null;");
		println("        try {");
		println("            byte idLength  = readByte(in);");
		println("            byte coMapType = readByte(in);");
 		println("            byte imgType   = readByte(in);");
		println("            short index    = readShort(in);");
		println("            short length   = readShort(in);");
		println("            byte coSize    = readByte(in);");
		println("            short xOrg     = readShort(in);");
		println("            short yOrg     = readShort(in);");
		println("            short width    = readShort(in);");
		println("            short height   = readShort(in);");
		println("            byte pixelSize  = readByte(in);");
		println("            byte attBits    = readByte(in);");
		println("            if (width <= 0 || height <= 0) {");
		println("                in.close();");
		println("                return null;");
		println("            }");
		if (pixelSize == 24)
		println("            bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);");
		if (pixelSize == 16 || pixelSize == 32)
		println("            bufImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);");
		if (pixelSize == 16)
		println("            readPixel16(in, bufImage, width, height);");
		if (pixelSize == 24)
		println("            readPixel24(in, bufImage, width, height);");
		if (pixelSize == 32)
		println("            readPixel32(in, bufImage, width, height);");
		println("            in.close();");
		println("        }");
		println("        catch (IOException ioe) {");
		println("            return null;");
		println("        } ");
		println("        return bufImage;");
		println("    }");
		println("    private BufferedImage loadTexture(String filename) {");
		println("        JarInputStream in = getCompressionInputStream(filename);");
		println("        if (in == null)");
		println("            return null;");
		println("        BufferedImage bufImage = loadTexture(in);");
		println("        try {");
		println("            in.close();");
		println("        }");
		println("        catch (IOException ioe) {}");
		println("        return bufImage;");
		println("    }");
		if (pixelSize == 16) {
		println("    private void readPixel16(InputStream in, BufferedImage bufImage, int width, int height) throws IOException {");
		println("        int     color;");
		println("        short   color16;");
		println("        byte    r, g, b;");
		println("        boolean useAlpha;");
		println("        for (int y=0; y<height; y++) {");
		println("            for (int x=0; x<width; x++) {");
		println("                color16 = readShort(in);");
		println("                b = (byte)( (float)((color16 & 0x7C00) >> 10) / 31.0f * 255.0f);");                
		println("                g = (byte)( (float)((color16 & 0x03E0) >>  5) / 31.0f * 255.0f);");                
		println("                r = (byte)( (float)((color16 & 0x001F) >>  0) / 31.0f * 255.0f);");    
		println("                useAlpha = ((color16 & 0x8000) == 0) ? true : false;");
		println("                color = 0;");
		println("                color |= (r << 16) & 0x00FF0000;");                
		println("                color |= (g <<  8) & 0x0000FF00;");                
		println("                color |= (b <<  0) & 0x000000FF;");    
		println("                if (useAlpha == false)");
		println("                    color |= 0xFF000000;");   
		println("                bufImage.setRGB(x, y, color);");            
		println("            }");
		println("        }");
		println("    }");
		}
		if (pixelSize == 24) {
		println("    private void readPixel24(InputStream in, BufferedImage bufImage, int width, int height) throws IOException {");
		println("        int color;");
		println("        for (int y=0; y<height; y++) {");
		println("            for (int x=0; x<width; x++) {");
		println("                color = 0;");
		println("                color |= (readByte(in) << 16) & 0x00FF0000;");                
		println("                color |= (readByte(in) <<  8) & 0x0000FF00;");                
		println("                color |= (readByte(in) <<  0) & 0x000000FF;");    
		println("                bufImage.setRGB(x, y, color);");            
		println("            }");
		println("        }");
		println("    }");
		}
		if (pixelSize == 32) {
		println("    private void readPixel32(InputStream in, BufferedImage bufImage, int width, int height) throws IOException {");
		println("        int color;");
		println("        for (int y=0; y<height; y++) {");
		println("            for (int x=0; x<width; x++) {");
		println("                color = 0;");
		println("                color |= (readByte(in) << 16) & 0x00FF0000;");                
		println("                color |= (readByte(in) <<  8) & 0x0000FF00;");                
		println("                color |= (readByte(in) <<  0) & 0x000000FF;");    
		println("                color |= (readByte(in) << 24) & 0xFF000000;");    
		println("                bufImage.setRGB(x, y, color);");            
		println("            }");
		println("        }");
		println("    }");
		}
	}
	
	///////////////////////////////////////////////
	//	save createTexure()
	///////////////////////////////////////////////

	private void saveCreateTextureMethod(int textureMode) {
    	int pixelSize = getTexturePixelSize(textureMode);
		println("    private Texture2D createTexture(int n) {");
		println("        String textureName = texture[n];");
		println("        if (textureName == null)");
		println("            return null;");
		println("        BufferedImage bufImage = loadTexture(texture[n]);");
		println("        if (bufImage == null)");
		println("            return null;");
		println("        int width  = bufImage.getWidth();");
		println("        int height = bufImage.getHeight();");
		if (pixelSize == 24) {
		println("        Texture2D tex2D = new Texture2D(Texture2D.BASE_LEVEL, Texture2D.RGB, width, height);");
		println("        ImageComponent2D imgComp = new ImageComponent2D(ImageComponent.FORMAT_RGB, bufImage);");
		}
		if (pixelSize == 16 || pixelSize == 32) {
		println("        Texture2D tex2D = new Texture2D(Texture2D.BASE_LEVEL, Texture2D.RGBA, width, height);");
		println("        ImageComponent2D imgComp = new ImageComponent2D(ImageComponent.FORMAT_RGBA, bufImage);");
		}
		println("        tex2D.setImage(0, imgComp);");
		println("        return tex2D;");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save Compression Methods
	///////////////////////////////////////////////

	private void saveDecompressionMethods(int coordMode, int normalMode, int colorMode, int texCoordMode) {
		println("    private boolean hasCompressionFile() {");
		println("        return (compressionFileName != null) ? true : false;");
		println("    }");
		println("    private CyberFormGeometryLoader	goemLoader = null;");
		println("    public JarInputStream getCompressionInputStream(URL url, String entryName) {");
		println("        JarInputStream compIn = null;");
		println("        try {");
		println("            if (goemLoader == null)");
		println("                goemLoader = new CyberFormGeometryLoader(url);");
		println("            goemLoader.rewind();");
		println("            compIn = new JarInputStream(goemLoader);");
		println("            JarEntry entry = compIn.getNextJarEntry();");
		println("            while (entry != null) {");
		println("                if (entryName.equals(entry.getName()) == true)");
		println("                    return compIn;");
		println("                entry = compIn.getNextJarEntry();");
		println("            }");
		println("        }");
		println("        catch (IOException ioe) {");
		println("            return null;");
		println("        } ");
		println("        return null;");
		println("    }");
		println("    private JarInputStream getCompressionInputStream(String entryName) {");
		println("        if (hasCompressionFile() == false)");
		println("            return null;");
		println("        try {");
		println("            URL compURL = null;");
		println("            if (docBase == null) {");
		println("                File file = new File(compressionFileName);");
		println("                compURL = file.toURL();");
		println("            }");
		println("            else");
		println("                compURL = new URL(docBase, compressionFileName);");
		println("            return getCompressionInputStream(compURL, entryName);");
		println("        }");
		println("        catch (MalformedURLException mURLe) {}");
		println("        return null;");
		println("    }");
		println("    public boolean closeCompressionInputStream(JarInputStream compIn) {");
		println("        try {");
		println("            compIn.close();");
		println("        }");
		println("        catch (IOException ioe) {");
		println("            return false;");
		println("        } ");
		println("        return true;");
		println("    }");
		println("    public byte readByte(InputStream in) {");
		println("        try {");
		println("            return (byte)in.read();");
		println("        } catch (IOException ioe) {}");
		println("        return 0;");
		println("    }");
		println("    public short readShort(InputStream in) {");
		println("        short value = 0;");
		println("        value |= ((short)readByte(in) << 0) & 0x00FF;");
		println("        value |= ((short)readByte(in) << 8) & 0xFF00;");
		println("        return value;");
		println("    }");
		println("    public int readInteger(InputStream in) {");
		println("        int value = 0;");
		println("        value |= ((int)readByte(in) << 24) & 0xFF000000;");
		println("        value |= ((int)readByte(in) << 16) & 0x00FF0000;");
		println("        value |= ((int)readByte(in) <<  8) & 0x0000FF00;");
		println("        value |= ((int)readByte(in) <<  0) & 0x000000FF;");
		println("        return value;");
		println("    }");
		println("    public int readInteger24(InputStream in) {");
		println("        int value = 0;");
		println("        value |= ((int)readByte(in) << 16) & 0x00FF0000;");
		println("        value |= ((int)readByte(in) <<  8) & 0x0000FF00;");
		println("        value |= ((int)readByte(in) <<  0) & 0x000000FF;");
		println("        return value;");
		println("    }");
		println("    public int readInteger16(InputStream in) {");
		println("        int value = 0;");
		println("        value |= ((int)readByte(in) <<  8) & 0x0000FF00;");
		println("        value |= ((int)readByte(in) <<  0) & 0x000000FF;");
		println("        return value;");
		println("    }");
		println("    public int readInteger8(InputStream in) {");
		println("        int value = 0;");
		println("        value |= ((int)readByte(in) <<  0) & 0x000000FF;");
		println("        return value;");
		println("    }");
		println("    public float readFloat(InputStream in) {");
		println("        return Float.intBitsToFloat(readInteger(in));");
		println("    }");
		if (colorMode == COLOR_RGB24) {
		println("    public void readColor24(JarInputStream in, float color[]) {");
		println("        color[0] = (float)(((int)readByte(in) & 0x000000FF)) / 255.0f;");
		println("        color[1] = (float)(((int)readByte(in) & 0x000000FF)) / 255.0f;");
		println("        color[2] = (float)(((int)readByte(in) & 0x000000FF)) / 255.0f;");
		println("    }");
		}
		if (colorMode == COLOR_RGBA32) {
		println("    public void readColor32(JarInputStream in, float color[]) {");
		println("        int intColor = readInteger(in);");
		println("        color[0] = (float)((intColor & 0x00FF0000) >> 16) / 255.0f;");
		println("        color[1] = (float)((intColor & 0x0000FF00) >>  8) / 255.0f;");
		println("        color[2] = (float)((intColor & 0x000000FF) >>  0) / 255.0f;");
		println("        color[3] = (float)((intColor & 0xFF000000) >> 24) / 255.0f;");
		println("    }");
		}
		if (colorMode == COLOR_RGBA16) {
		println("    public void readColor16(JarInputStream in, float color[]) {");
		println("        short shortColor = readShort(in);");
		println("        color[0] = (float)((shortColor & 0x7C00) >> 10) / 31.0f;");
		println("        color[1] = (float)((shortColor & 0x03E0) >>  5) / 31.0f;");
		println("        color[2] = (float)((shortColor & 0x001F) >>  0) / 31.0f;");
		println("        color[3] = ((shortColor & 0x8000) != 0) ? 1.0f : 0.0f;");
		println("    }");
		}
	}

	///////////////////////////////////////////////
	//	save createGeometryArray()
	///////////////////////////////////////////////

	private void saveCreateGeometryMethod() {
		println("    private Geometry createGeometry(int n) {");
		println("        GeometryArray geom = null;");
		println("        switch (geomType[n]) {");
		println("        case GEOM_INDEXED_TRIANGLE_ARRAY  : geom = new IndexedTriangleArray(vertexCount[n], vertexFormat[n], indexCount[n]); break;");
		println("        case GEOM_TRIANGLE_ARRAY          : geom = new TriangleArray       (vertexCount[n], vertexFormat[n]);                break;");
		println("        case GEOM_QUAD_ARRAY              : geom = new QuadArray           (vertexCount[n], vertexFormat[n]);                break;");
		println("        }");
		println("        if (geom == null)");
		println("            return null;");
		println("        for (int i=0; i<vertexCount[n]; i++) {");
		println("            if ((vertexFormat[n] & GeometryArray.COORDINATES) != 0 && i < coord[n].length)");
		println("                geom.setCoordinate(i, coord[n][i]);");
		println("            if (((vertexFormat[n] & GeometryArray.COLOR_3) != 0 || (vertexFormat[n] & GeometryArray.COLOR_4) != 0) && i < color[n].length)");
		println("                geom.setColor(i, color[n][i]);");
		println("            if ((vertexFormat[n] & GeometryArray.NORMALS) != 0 && i < normal[n].length)");
		println("                geom.setNormal(i, normal[n][i]);");
		println("            if ((vertexFormat[n] & GeometryArray.TEXTURE_COORDINATE_2) != 0 && i < texCoord[n].length)");
		println("                geom.setTextureCoordinate(i, texCoord[n][i]);");
		println("        }");
		println("        for (int i=0; i<indexCount[n]; i++) {");
		println("            if (geom instanceof IndexedGeometryArray) {");
		println("                IndexedGeometryArray idxGeom = (IndexedGeometryArray)geom;");
		println("                if ((vertexFormat[n] & GeometryArray.COORDINATES) != 0 && i < coordIndex[n].length)");
		println("                    idxGeom.setCoordinateIndex(i, coordIndex[n][i]);");
		println("                if (((vertexFormat[n] & GeometryArray.COLOR_3) != 0 || (vertexFormat[n] & GeometryArray.COLOR_4) != 0) && i < colorIndex[n].length)");
		println("                    idxGeom.setColorIndex(i, colorIndex[n][i]);");
		println("                if ((vertexFormat[n] & GeometryArray.NORMALS) != 0 && i < normalIndex[n].length)");
		println("                    idxGeom.setNormalIndex(i, normalIndex[n][i]);");
		println("                if ((vertexFormat[n] & GeometryArray.TEXTURE_COORDINATE_2) != 0 && i < texCoordIndex[n].length)");
		println("                    idxGeom.setTextureCoordinateIndex(i, texCoordIndex[n][i]);");
		println("            }");
		println("        }");
		println("        return geom;");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save createShape3D()
	///////////////////////////////////////////////

	private void saveCreateShapeMethod(int textureMode) {
		println("    public TransformGroup createShape(int n) {");
		println("        TransformGroup transGroup = createTransformGroup(n);");
		println("        Shape3D        shape3d    = new Shape3D();");
		println("        Appearance     app        = new Appearance();");
		println("        Material       mat        = createMaterial(n);");
		println("        Geometry       geom       = createGeometry(n);");
		if (textureMode != TEXTURE_NONE) 
		println("        Texture2D      tex        = createTexture(n);");
		println("        transGroup.addChild(shape3d);");
		println("        shape3d.setAppearance(app);");
		println("        app.setMaterial(mat);");
		if (textureMode != TEXTURE_NONE) {
		println("        if (tex != null)");
		println("            app.setTexture(tex);");
		}
		println("        shape3d.setGeometry(geom);");
		println("        return transGroup;");
		println("    }");
	}

	///////////////////////////////////////////////
	//	save getShape3D()
	///////////////////////////////////////////////

	private void saveGetShapeMethod() {
		println("    public TransformGroup getShape(int n) {");
		println("        if (mShapeTransGroup[n] == null)");
		println("            mShapeTransGroup[n] = createShape(n);");
		println("        return mShapeTransGroup[n];");
		println("    }");
	}

	///////////////////////////////////////////////
	//	Header
	///////////////////////////////////////////////

	private void saveHeader() {
		Calendar c = new GregorianCalendar();
		println("/************************************************************");
		println("*");
		println("*    " + getClassName() + ".java [" + c.get(c.YEAR) + "/" + c.get(c.MONTH) + "/" + c.get(c.DATE) + " " + c.get(c.HOUR) + ":" + c.get(c.MINUTE) + ":" + c.get(c.SECOND) + "]");
		println("*");
		println("*    This file is generated by CyberForm for Java.");
		println("*");
		println("************************************************************/");
		println("");
		println("import java.io.*;");
		println("import java.net.*;");
		println("import java.awt.image.*;");
		println("import java.applet.Applet;");
		println("import java.util.jar.*;");
		println("");
		println("import javax.media.j3d.*;");
		println("import javax.vecmath.*;");
		println("");
	}

	///////////////////////////////////////////////
	//	Constants
	///////////////////////////////////////////////

	private void saveConstants() {
		println("    private final static int GEOM_NONE                   = 0;");
		println("    private final static int GEOM_INDEXED_TRIANGLE_ARRAY = 1;");
		println("    private final static int GEOM_TRIANGLE_ARRAY         = 2;");
		println("    private final static int GEOM_QUAD_ARRAY             = 3;");
	}

	///////////////////////////////////////////////
	//	Compression File Name
	///////////////////////////////////////////////

	private void saveCompressionFileName() {
		println("    private final static String compressionFileName = \"" + getCompressionOutputFileName() + "\";");
	}

	////////////////////////////////////////////////
	//	Directory/Directory
	////////////////////////////////////////////////
	
	private String mBaseDirectory = null;

	public void setBaseDirectory(String baseDir) {
		mBaseDirectory = baseDir;
	}
	
	public String getBaseDirectory() {
		return mBaseDirectory;
	}

	////////////////////////////////////////////////
	//	Compression
	////////////////////////////////////////////////
	
	JarOutputStream	mCompressionOutputStream	= null;
	
	public String getCompressionOutputFileName() {
		return new String(getClassName() + ".bin");
	}
	
	public File getCompressionOutputFile() {
		return new File(getBaseDirectory() + File.separator + getCompressionOutputFileName());
	}

	public void setCompressionOutputStream(JarOutputStream compOutStream) {
		mCompressionOutputStream = compOutStream;
	}

	public JarOutputStream getCompressionOutputStream() {
		return mCompressionOutputStream;
	}
		
	public JarOutputStream openCompressionOutputStream() {
		File compFile = getCompressionOutputFile();
		 
		if (compFile.exists() == true)
			compFile.delete();
		 
		JarOutputStream compOutStream = null;	
		try {
			FileOutputStream outStream = new FileOutputStream(compFile);
			compOutStream = new JarOutputStream(outStream);
		}
		catch (FileNotFoundException fnfe) {
			return null;
		}
		catch (IOException ioe) {
			return null;
		}
		
		return compOutStream;
	}

	public boolean putNextCompressionOutputStreamEntry(JarOutputStream compOutStream, String name) {
		try {
			ZipEntry zipEntry = new ZipEntry(name);
			compOutStream.putNextEntry(zipEntry);
		}
		catch (IOException ioe) {
			return false;
		}
		return true;
	}	

	public JarOutputStream openNewCompressionOutputStream(String name) {
		 JarOutputStream outStream = getCompressionOutputStream();
		 putNextCompressionOutputStreamEntry(outStream, name);
		 return outStream;
	}	

	public boolean closeCompressionOutputStream(JarOutputStream compOutStream) {
		try {
			compOutStream.close();
		}
		catch (IOException ioe) {
			return false;
		}
		return true;
	}

	////////////////////////////////////////////////
	//	OutputStream
	////////////////////////////////////////////////
	
	private void writeByte(OutputStream out, byte data) {
		try {
			out.write(data);
		} catch (IOException ioe) {}
	}

	private void writeShort(OutputStream out, short data) {
		try {
			out.write((byte)((data & 0x00FF) >>  0));
			out.write((byte)((data & 0xFF00) >>  8));
		} catch (IOException ioe) {}
	}

	private void writeInteger(OutputStream out, int data) {
		try {
			out.write((byte)(((data & 0xFF000000) >> 24) & 0x000000FF));
			out.write((byte)(((data & 0x00FF0000) >> 16) & 0x000000FF));
			out.write((byte)(((data & 0x0000FF00) >>  8) & 0x000000FF));
			out.write((byte)(((data & 0x000000FF) >>  0) & 0x000000FF));
		} catch (IOException ioe) {}
	}

	private void writeInteger24(OutputStream out, int data) {
		try {
			out.write((byte)(((data & 0x00FF0000) >> 16) & 0x000000FF));
			out.write((byte)(((data & 0x0000FF00) >>  8) & 0x000000FF));
			out.write((byte)(((data & 0x000000FF) >>  0) & 0x000000FF));
		} catch (IOException ioe) {}
	}

	private void writeInteger16(OutputStream out, int data) {
		try {
			out.write((byte)(((data & 0x0000FF00) >>  8) & 0x000000FF));
			out.write((byte)(((data & 0x000000FF) >>  0) & 0x000000FF));
		} catch (IOException ioe) {}
	}

	private void writeInteger8(OutputStream out, int data) {
		try {
			out.write((byte)(((data & 0x000000FF) >>  0) & 0x000000FF));
		} catch (IOException ioe) {}
	}

	private void writeFloat(OutputStream out, float data) {
		writeInteger(out, Float.floatToIntBits(data));	
	}
	
	private void writeRGB24Color(OutputStream out, int data) {
		try {
			out.write((byte)(((data & 0x00FF0000) >> 16) & 0x000000FF));
			out.write((byte)(((data & 0x0000FF00) >>  8) & 0x000000FF));
			out.write((byte)(((data & 0x000000FF) >>  0) & 0x000000FF));
		} catch (IOException ioe) {}
	}

	private void writeRGB32Color(OutputStream out, int data) {
		writeInteger(out, data);
	}

	private void writeRGB16Color(OutputStream out, short data) {
		writeShort(out, data);
	}

	///////////////////////////////////////////////
	//	CyberFormGeometryLoader.java
	///////////////////////////////////////////////

	private static String cyberFormGeometryLoaderFileName	= "CyberFormGeometryLoader.java";

	public static String getGeometryLoaderFileName() {
		return cyberFormGeometryLoaderFileName;
	}
	
	private String getGeometryLoaderPathName() {
		String pathName = getBaseDirectory();
		String fileName =  pathName + File.separator + cyberFormGeometryLoaderFileName;
		return fileName;
	}

	private boolean hasGeometryLoaderFile() {
		File	file = new File(getGeometryLoaderPathName());	
		return file.exists();
	}
	
	private boolean saveGeometryLoaderFile() {
		String fileName = getGeometryLoaderPathName();
		FileOutputStream outStream;
		try {
			outStream = new FileOutputStream(fileName);
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("FileNotFoundException = " + fileName);
			return false;
		}
		
		PrintWriter pw = new PrintWriter(outStream);
		setPrintWriter(pw);
		
		Calendar c = new GregorianCalendar();
		println("/************************************************************");
		println("*");
		println("*    CyberFormGeometryLoader.java [" + c.get(c.YEAR) + "/" + c.get(c.MONTH) + "/" + c.get(c.DATE) + " " + c.get(c.HOUR) + ":" + c.get(c.MINUTE) + ":" + c.get(c.SECOND) + "]");
		println("*");
		println("*    This file is generated by CyberForm for Java.");
		println("*");
		println("************************************************************/");
		println("import java.io.*;");
		println("import java.net.*;");
		println("public class CyberFormGeometryLoader extends InputStream {");
		println("    private byte geomData[] = new byte[0];");
		println("    private int  pos;");
		println("    public CyberFormGeometryLoader(URL url) {");
		println("        try {");
		println("            BufferedInputStream in = new BufferedInputStream(url.openStream());");
		println("            int dataSize = in.available();");
		println("            geomData = new byte[dataSize];");
		println("            int readOnceDataSize = 1024*32;");
		println("            int readDataSize = 0;");
		println("            while (readDataSize < dataSize) {");
		println("                int remainDataSize = dataSize - readDataSize;");
		println("                int readSize = (remainDataSize < readOnceDataSize) ? remainDataSize : readOnceDataSize;");
		println("                int size = in.read(geomData, readDataSize, readSize);");
		println("                if (size < 0)");
		println("                    break;");
		println("                readDataSize += size;");
		println("            }");
		println("        }");
		println("        catch (IOException ioe) {");
		println("            geomData = new byte[0];");
		println("            return;");
		println("        }");
		println("        rewind();");
		println("    }");
		println("    public void rewind() {");
		println("        pos = 0;");
		println("    }");
		println("    public int read() throws IOException {");
		println("        if ((geomData.length-1) < pos)");
		println("            return -1;");
		println("        int data = (int)geomData[pos] & 0xFF;");
		println("        pos++;");
		println("        return data;");
		println("    }");
		println("    public int available() throws IOException {");
		println("        return geomData.length - pos;");
		println("    }");
		println("    public void close() throws IOException {");
		println("        rewind();");
		println("    }");
		println("}");

		pw.close();
		
		try {
			outStream.close();
		}
		catch (IOException ioe) {}
		
		return true;
	}

	///////////////////////////////////////////////
	//	save
	///////////////////////////////////////////////
	
	private void save(int coordMode, int normalMode, int colorMode, int textureMode, int texCoordMode) {

		JarOutputStream jarOutStrerm = openCompressionOutputStream();
		
		if (jarOutStrerm == null)
			return;
			
		setCompressionOutputStream(jarOutStrerm);

		saveHeader();
		
		println("public class " + getClassName() + " {");

		saveConstants();
		
		saveCompressionFileName();
		 
		saveGeometryTypes();
		saveTransformMatrices();
		saveMaterials();
		saveTextures(textureMode);
		saveVertexCounts();
		saveVertexFormats(colorMode);
		
		if (coordMode != COORD_32)
			saveCoordinatePrecisions(coordMode);
		if (normalMode != NORMAL_32)
			saveNormalPrecisions(normalMode);
		if (texCoordMode != TEXCOORD_32)
			saveTexCoordPrecisions(texCoordMode);
			
		saveCoordinates(coordMode);
		saveColors(colorMode);
		saveNormals(normalMode);
		saveTextureCoordinates(texCoordMode);
		
		saveIndexCounts();
		
		saveCoordinateIndices();
		saveColorIndices();
		saveNormalIndices();
		saveTextureCoordinateIndices();

		if (coordMode != COORD_32 || normalMode != NORMAL_32 || texCoordMode != TEXCOORD_32)
			savePrecisionMethods();
		
		saveDecompressionMethods(coordMode, normalMode, colorMode, texCoordMode);
		saveSetDecompressionDataMethods(coordMode, normalMode, colorMode, texCoordMode);
		if (textureMode != TEXTURE_NONE) 
			saveLoadTextureMethod(textureMode);
		
		saveConstructor(coordMode, normalMode, colorMode, textureMode, texCoordMode);
		
		saveGetShapeCountMethod();
		saveCreateTransformGroupMethod();
		saveCreateMaterialMethod();
		if (textureMode != TEXTURE_NONE) 
			saveCreateTextureMethod(textureMode);
		saveCreateGeometryMethod();
		saveCreateShapeMethod(textureMode);
		saveGetShapeMethod();
				
		println("}");

		closeCompressionOutputStream(jarOutStrerm);
	}

	public boolean save(OutputStream outputStream, String className, int coordMode, int normalMode, int colorMode, int textureMode, int texCoordMode) {
		if (hasGeometryLoaderFile() == false) 
			saveGeometryLoaderFile();

		setClassName(className);
		PrintWriter pw = new PrintWriter(outputStream);
		setPrintWriter(pw);
		save(coordMode, normalMode, colorMode, textureMode, texCoordMode);
		pw.close();
		return true;
	}

	public boolean save(String pathName, String className, int coordMode, int normalMode, int colorMode, int textureMode, int texCoordMode) {
		Debug.message("PathName    = " + pathName);
		Debug.message("ClassName   = " + className);
		Debug.message("colorMode   = " + colorMode);
		Debug.message("textureMode = " + textureMode);
			
		setBaseDirectory(pathName);
		
		String fileName = pathName + File.separator + className + ".java";
		
		Debug.message("FileName = " + fileName);
		
		boolean ret = false;
		
		try {
			FileOutputStream outputStream = new FileOutputStream(fileName);
			ret = save(outputStream, className, coordMode, normalMode, colorMode, textureMode, texCoordMode);
			outputStream.close();
		}
		catch (IOException e) {
			Debug.warning("Couldn't open the file (" + fileName + ")");
			return false;
		}
		
		return true;
	}

	public boolean save(File file, int coordMode, int normalMode, int colorMode, int textureMode, int texCoordMode) {
		File absFile = file.getAbsoluteFile();
		String parent = absFile.getParent();
		String name = absFile.getName();
		int idx = name.lastIndexOf(".");
		String className;
		if (0 < idx) 
			className = new String(name.getBytes(), 0, idx);
		else
			className = name; 
		return save(parent, className, coordMode, normalMode, colorMode, textureMode, texCoordMode);
	}
}
