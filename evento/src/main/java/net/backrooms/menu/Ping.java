package net.backrooms.menu;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import javax.naming.directory.Attribute;
import javax.naming.directory.InitialDirContext;

/**
 * Estado del servidor del evento (jugadores y latencia) con el ping de la
 * lista de servidores, en segundo plano y como mucho cada 30 s.
 */
public final class Ping {
	public record Estado(boolean online, int latencia, int jugadores, int maximo) {
	}

	private static volatile Estado ultimo;
	private static volatile long siguiente;

	private Ping() {
	}

	public static Estado estado(Evento e) {
		long ahora = System.currentTimeMillis();
		if (e.tieneServidor() && ahora >= siguiente) {
			siguiente = ahora + 30_000;
			// Hilo propio y nada de clases del juego dentro: desde el pool comun de
			// Java el ServiceLoader de Minecraft falla y deja rota su clase para
			// siempre (y con ella la conexion al servidor).
			Thread hilo = new Thread(() -> ultimo = medir(e.host, e.puerto), "backrooms-ping");
			hilo.setDaemon(true);
			hilo.start();
		}
		return ultimo;
	}

	private static void varint(DataOutputStream out, int v) throws IOException {
		while ((v & ~0x7F) != 0) {
			out.writeByte((v & 0x7F) | 0x80);
			v >>>= 7;
		}
		out.writeByte(v);
	}

	private static int leerVarint(DataInputStream in) throws IOException {
		int v = 0;
		for (int i = 0; i < 5; i++) {
			int b = in.readUnsignedByte();
			v |= (b & 0x7F) << (7 * i);
			if ((b & 0x80) == 0) {
				return v;
			}
		}
		throw new IOException("VarInt demasiado largo");
	}

	/** Registro SRV _minecraft._tcp, como hace la lista de servidores: {host, puerto} o null. */
	private static String[] srv(String host) {
		try {
			Hashtable<String, String> env = new Hashtable<>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			env.put("com.sun.jndi.dns.timeout.retries", "1");
			Attribute a = new InitialDirContext(env).getAttributes("_minecraft._tcp." + host, new String[] {"SRV"}).get("srv");
			if (a == null) {
				return null;
			}
			String[] p = a.get().toString().split(" ", 4);
			String destino = p[3].endsWith(".") ? p[3].substring(0, p[3].length() - 1) : p[3];
			return new String[] {destino, p[2]};
		} catch (Exception ex) {
			return null;
		}
	}

	private static Estado medir(String host, int puerto) {
		try {
			String destino = host;
			int puertoReal = puerto;
			if (puerto == 25565) {
				String[] srv = srv(host);
				if (srv != null) {
					destino = srv[0];
					puertoReal = Integer.parseInt(srv[1]);
				}
			}
			try (Socket s = new Socket()) {
				long inicio = System.currentTimeMillis();
				s.connect(new InetSocketAddress(destino, puertoReal), 5000);
				s.setSoTimeout(5000);
				DataOutputStream out = new DataOutputStream(s.getOutputStream());
				ByteArrayOutputStream hs = new ByteArrayOutputStream();
				DataOutputStream h = new DataOutputStream(hs);
				h.writeByte(0x00);
				varint(h, -1);
				byte[] hb = host.getBytes(StandardCharsets.UTF_8);
				varint(h, hb.length);
				h.write(hb);
				h.writeShort(puerto);
				varint(h, 1);
				varint(out, hs.size());
				out.write(hs.toByteArray());
				out.writeByte(0x01);
				out.writeByte(0x00);
				out.flush();
				DataInputStream in = new DataInputStream(s.getInputStream());
				leerVarint(in);
				leerVarint(in);
				byte[] json = new byte[leerVarint(in)];
				in.readFully(json);
				int latencia = (int) (System.currentTimeMillis() - inicio);
				JsonObject o = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
				JsonObject p = o.has("players") ? o.getAsJsonObject("players") : new JsonObject();
				return new Estado(true, latencia, p.has("online") ? p.get("online").getAsInt() : 0, p.has("max") ? p.get("max").getAsInt() : 0);
			}
		} catch (Exception ex) {
			return new Estado(false, 0, 0, 0);
		}
	}
}
