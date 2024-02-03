/******************************************************************
*
*	CyberToolBox for Java
*
*	Copyright (C) Satoshi Konno 1998
*
*	File:	Message.java
*
******************************************************************/

import java.awt.*;
import javax.swing.*;

public class Message extends Object {

	static int YES_OPTION = JOptionPane.YES_OPTION;
	
	static public void beep() {
		Toolkit.getDefaultToolkit().beep();
	}

	static public void showWarningDialog(Component parentComponent, String message) {
		beep();
		JOptionPane.showMessageDialog(parentComponent, message, "Warning", JOptionPane.WARNING_MESSAGE);
	}

	static public int showConfirmDialog(Component parentComponent, String message) {
		return JOptionPane.showConfirmDialog(parentComponent, message, "", JOptionPane.OK_CANCEL_OPTION);
	}

	public static final boolean enabled = true;
	
	public static final void debug(String s) {
		if (enabled == true)
			System.out.println("CyberForm message : " + s);
	}
	public static final void warning(String s) {
		if (enabled == true)
			System.out.println("CyberForm warning : " + s);
	}

}
