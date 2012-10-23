package net.ion.radon.repository.util;

import java.util.Map.Entry;

import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.internal.LazilyParsedNumber;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class JSONUtil {

	public static DBObject toDBObject(JsonObject json) {
		BasicDBObject result = new BasicDBObject() ;
		
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			result.put(entry.getKey(), toPreferObject(entry.getValue())) ;
		}
		return result;
	}

	public static Object toPreferObject(JsonElement jsonElement) {
		if (jsonElement.isJsonArray()) {
			JsonElement[] jeles = jsonElement.getAsJsonArray().toArray();
			BasicDBList list = new BasicDBList() ;
			for (JsonElement jele : jeles) {
				list.add(toPreferObject(jele));
			}
			return list;
		} else if (jsonElement.isJsonObject()) {
			BasicDBObject newDBO = new BasicDBObject() ;
			for (Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
				newDBO.put(entry.getKey(), toPreferObject(entry.getValue())) ;
			}
			return newDBO;
		} else if (jsonElement.isJsonPrimitive()) {
			if (jsonElement.getAsJsonPrimitive().getValue() instanceof LazilyParsedNumber) {
				long longValue = ((LazilyParsedNumber) jsonElement.getAsJsonPrimitive().getValue()).longValue();
				return longValue ;
			} else {
				return jsonElement.getAsJsonPrimitive().getValue();
			}
		} else if (jsonElement.isJsonNull()) {
			return null;
		} else {
			return null;
		}
	}

}
