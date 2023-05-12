package io.github.happysnaker.hbotcore.utils;

import com.alibaba.fastjson.JSONObject;

import java.util.*;

/**
 * 提供字段自动转换，嵌套 MAP 调用等实用功能
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MapGetter implements Cloneable {
    private Map<Object, Object> map;

    public MapGetter() {
        this.map = new HashMap<>();
    }

    @Override
    public String toString() {
        return "MapGetter{" +
                "map=" + map +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapGetter mapGetter = (MapGetter) o;
        return Objects.equals(map, mapGetter.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    public MapGetter(Object obj) {
        if (obj instanceof  Map) {
            this.map = (Map) obj;
        } else if (obj instanceof MapGetter) {
            this.map = ((MapGetter) obj).map;
        } else {
            try {
                this.map = JSONObject.parseObject(JSONObject.toJSONString(obj), Map.class);
            } catch (Exception e) {
                throw new RuntimeException("不合法的构造参数");
            }
        }
        if (this.map == null) {
            this.map = new HashMap<>();
        }
    }

    public void put(Object key, Object val) {
        this.map.put(key, val);
    }

    public Map getMap() {
        return this.map;
    }

    public MapGetter(Map map) {
        this.map = map;
        if (this.map == null) {
            this.map = new HashMap<>();
        }
    }

    public MapGetter(MapGetter mg) {
        this.map = mg.map;
    }


    @Override
    public MapGetter clone() throws CloneNotSupportedException {
        MapGetter clone = (MapGetter) super.clone();
        clone.map = new HashMap<>(this.map);
        return clone;
    }

    public MapGetter getMapGetter(Object key) {
        if (!map.containsKey(key)) {
            return null;
        }
        if (map.get(key) == null) {
            return null;
        }
        return new MapGetter((Map) map.get(key));
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public int size() {
        return map.size();
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public<T> T get(Object key, Class<T> tClass) {
        return (T) map.get(key);
    }

    public<T> T getOrDefault(Object key, T defVal, Class<T> tClass) {
        return (T) map.getOrDefault(key, defVal);
    }

    public String getString(Object key) {
        return getString(key, false);
    }

    public String getString(Object key, boolean toString) {
        if (!map.containsKey(key)) {
            return null;
        }
        if (toString) {
            return map.get(key).toString();
        }
        return (String) map.get(key);
    }

    public String getStringOrDefault(Object key, String val) {
        if (!map.containsKey(key)) {
            return val;
        }
        return (String) map.get(key);
    }

    public long getLong(Object key) {
        return Long.parseLong(map.get(key).toString());
    }

    public<T> T getItemFromList(Object key, int index, Class<T> tClass) {
        List<T> ts = getList(key, tClass);
        if (ts == null || ts.size() <= index) {
            return null;
        }
        return ts.get(index);
    }

    public int getInt(Object key) {
        if (!map.containsKey(key)) {
            return 0;
        }
        return Integer.parseInt(map.get(key).toString());
    }

    public double getDouble(Object key) {
        if (!map.containsKey(key)) {
            return 0;
        }
        return Double.parseDouble(map.get(key).toString());
    }

    public float getFloat(Object key) {
        if (!map.containsKey(key)) {
            return 0;
        }
        return Float.parseFloat(map.get(key).toString());
    }

    public List getList(Object key) {
        if (!map.containsKey(key)) {
            return null;
        }
        return (List) map.get(key);
    }

    public<T> List<T> getList(Object key, Class<T> tClass) {
        if (!map.containsKey(key)) {
            return null;
        }
        return (List<T>) map.get(key);
    }

    public<T> List<T> getListOrWrapperSingleton(Object key, Class<T> tClass) {
        if (!map.containsKey(key)) {
            return null;
        }
        if (!(map.get(key) instanceof List)) {
            return (List<T>) List.of(map.get(key));
        }
        return (List<T>) map.get(key);
    }

    public List<MapGetter> getMapGetterList(Object key) {
        List<Map> maps = getList(key, Map.class);
        if (maps == null) {
            return null;
        }
        List<MapGetter> ret = new ArrayList<>();
        maps.forEach(m -> ret.add(new MapGetter(m)));
        return ret;
    }

    public boolean getBoolean(Object key) {
        if (!map.containsKey(key)) {
            return false;
        }
        return Boolean.parseBoolean(map.get(key).toString());
    }

    public String getStringOrDefault(String key, String val, boolean toString) {
        if (!map.containsKey(key)) {
            return val;
        }
        if (toString) {
            return map.get(key).toString();
        }
        return (String) map.get(key);
    }
}
