package com.secchat.ui;

import java.net.http.HttpClient;
import java.sql.SQLException;

import com.secchat.client.Client;
import com.secchat.client.ClientData;
import com.secchat.client.ClientData.DHAlgorithms;
import com.secchat.clientdata.ILocalDS;
import com.secchat.clientdata.SecureLocalDS;

public class JClientMain {

	public static void main(String[] args) throws ClassNotFoundException, SQLException {
//		ILocalDS ds = new LocalDS("jdbc:sqlite:usrs.db", "jdbc:sqlite:msgs.db");
		ILocalDS ds = new SecureLocalDS("123456whatever!@£$%^&*&((", "jdbc:sqlite:usrs.db", "jdbc:sqlite:msgs.db");
		System.out.println("Using local storage: " + ds.getClass().getCanonicalName());
		
		
		ClientData data = new ClientData(ds, DHAlgorithms.EC);
		Client client = new Client(args.length > 0 ? args[0] : "http://localhost:8080", data,
				HttpClient.newHttpClient());
		
		ChatUI myGui = new ChatUI(client, data);
		myGui.show();
		myGui.getUserControler().init();
	}
}
