package loader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads all plugins
 * 
 * @author Daniel Hohmann
 *
 */
public class PluginLoader {

	private static List<Object> plugins = new ArrayList<>();

	static {
		// load content of config
		List<String> features = null;
		try {
			features = Files.readAllLines(Paths.get("src" + FileSystems.getDefault().getSeparator() + "config.txt"),
					Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Feature Exception");
		}

		for (String feature : features) {
			try {
				plugins.add(ClassLoader.getSystemClassLoader().loadClass(feature).newInstance());
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				System.out.println("Plugin Exception");
			} catch (ClassNotFoundException e) {
				System.out.println(">> Error occured:  ClassNotFoundException"
						+ "\n\tCheck config.txt file for not selected features");
			}
		}

	}

	public static <T> List<T> load(Class<T> cl) {
		final List<T> list = new ArrayList<>();
		for (Object obj : plugins) {
			if (cl.isInstance(obj)) {
				list.add(cl.cast(obj));
			}
		}
		return list;
	}
}
