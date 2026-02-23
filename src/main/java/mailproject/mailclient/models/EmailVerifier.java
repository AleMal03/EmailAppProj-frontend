package mailproject.mailclient.models;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Callable;


public class EmailVerifier implements Callable<Boolean> {
	private final String email;
	private final String currentUser;

	public EmailVerifier(String email, String currentUser) {
		this.email = email;
		this.currentUser = currentUser;
	}

	public Boolean call(){
		try{
			return emailExists();
		} catch (RuntimeException e) {
			return false;
		}
	}

	private boolean emailExists(){
		final String serverIp = "127.0.0.1";
		final int serverPort = 5000;
		ServerRequest req = new ServerRequest(currentUser, "VER_EML", email);
		ServerResponse res = null;

		// Serializzo la richiesta in JSON
		Gson gson = new Gson();
		String reqJson = gson.toJson(req);

		try(Socket socket = new Socket(serverIp, serverPort);
		    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			Scanner in = new Scanner(socket.getInputStream());){

			out.println(reqJson);
			res = gson.fromJson(in.nextLine(),  ServerResponse.class);
		}
		catch (IOException | NoSuchElementException e){
			System.err.println(e.getMessage());
		}
		catch (JsonSyntaxException e){
			throw new RuntimeException("Errore nella ricezione del messaggio Json " + e);
		}

		if(res != null){
			return res.isSuccess();
		}

		throw new RuntimeException("Comunicazione col server fallita");
	}
}
