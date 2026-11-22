package amaguri.shiritori;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** 翻訳キー → 名前と読み。クラスパスの shiritori/readings.json から1回だけ読む（無ければ空）。 */
public final class Readings {
	public record Entry(String name, String reading) {}

	private static final String PATH = "/shiritori/readings.json";
	private static Map<String, Entry> map;

	private Readings() {}

	public static synchronized Entry get(String translationKey) {
		if (map == null) map = load();
		return map.get(translationKey);
	}

	private static Map<String, Entry> load() {
		Map<String, Entry> result = new HashMap<>();
		try (InputStream in = Readings.class.getResourceAsStream(PATH)) {
			if (in == null) {
				ShiritoriMod.LOGGER.warn("readings.json not found; dictionary is empty");
				return result;
			}
			JsonElement root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			if (root.isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : root.getAsJsonObject().entrySet()) {
					if (!e.getValue().isJsonObject()) continue;
					JsonObject o = e.getValue().getAsJsonObject();
					if (!o.has("name") || !o.has("reading")) continue;
					result.put(e.getKey(), new Entry(o.get("name").getAsString(), o.get("reading").getAsString()));
				}
			}
			ShiritoriMod.LOGGER.info("Loaded {} readings", result.size());
		} catch (Exception ex) {
			ShiritoriMod.LOGGER.error("Failed to load readings.json", ex);
		}
		return result;
	}
}
