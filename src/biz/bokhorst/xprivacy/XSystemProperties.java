package biz.bokhorst.xprivacy;

import java.util.ArrayList;
import java.util.List;

import android.os.Binder;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XSystemProperties extends XHook {
	private Methods mMethod;
	private String mPropertyName;

	private XSystemProperties(Methods method, String restrictionName, String propertyName) {
		super(restrictionName, method.name(), propertyName);
		mMethod = method;
		mPropertyName = propertyName;
	}

	public String getClassName() {
		return "android.os.SystemProperties";
	}

	// public static String get(String key)
	// public static String get(String key, String def)
	// public static boolean getBoolean(String key, boolean def)
	// public static int getInt(String key, int def)
	// public static long getLong(String key, long def)
	// frameworks/base/core/java/android/os/SystemProperties.java

	private enum Methods {
		get, getBoolean, getInt, getLong
	};

	public static List<XHook> getInstances() {
		List<XHook> listHook = new ArrayList<XHook>();
		String[] props = new String[] { "%imei", "%hostname", "%serialno", "%macaddr", "%cid" };
		for (String prop : props)
			for (Methods getter : Methods.values())
				listHook.add(new XSystemProperties(getter, PrivacyManager.cIdentification, prop));
		return listHook;
	}

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		// Do nothing
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		String key = (param.args.length > 0 ? (String) param.args[0] : null);
		if (key != null)
			if (mPropertyName.startsWith("%") ? key.contains(mPropertyName.substring(1)) : key.equals(mPropertyName))
				if (mMethod == Methods.get) {
					if (param.getResult() != null && isRestricted(param, mPropertyName))
						param.setResult(PrivacyManager.getDefacedProp(Binder.getCallingUid(), mPropertyName));
				} else if (param.args.length > 1) {
					if (isRestricted(param, mPropertyName))
						param.setResult(param.args[1]);
				} else
					Util.log(this, Log.WARN, "Unknown method=" + param.method.getName());
	}
}
