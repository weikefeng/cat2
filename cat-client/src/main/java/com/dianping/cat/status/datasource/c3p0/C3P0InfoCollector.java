package com.dianping.cat.status.datasource.c3p0;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import org.unidal.helper.Properties;

import com.dianping.cat.status.datasource.DataSourceCollector;
import com.dianping.cat.status.datasource.DatabaseParser.Database;

public class C3P0InfoCollector extends DataSourceCollector {

	private final String PREFIX_KEY = "c3p0";

	private Map<String, Number> doCollect() {
		Map<String, Number> map = new LinkedHashMap<String, Number>();
		Map<String, C3P0MonitorInfo> c3P0MonitorInfoMap = getC3P0MonitorInfoMap();
		String detail = Properties.forString().fromEnv().fromSystem().getProperty("CAT_DATASOURCE_DETAIL", "false");

		for (Map.Entry<String, C3P0MonitorInfo> entry : c3P0MonitorInfoMap.entrySet()) {
			String dataSourceName = entry.getKey();
			C3P0MonitorInfo value = entry.getValue();

			map.put(PREFIX_KEY + SPLIT + dataSourceName + ".busy_connection", value.getNumBusyConnections());
			map.put(PREFIX_KEY + SPLIT + dataSourceName + ".total_connection", value.getNumConnections());
			map.put(PREFIX_KEY + SPLIT + dataSourceName + ".idle_connection", value.getNumIdleConnections());

			if ("true".equals(detail)) {
				map.put(PREFIX_KEY + SPLIT + dataSourceName + ".failed_checkin", value.getNumFailedCheckins());
				map.put(PREFIX_KEY + SPLIT + dataSourceName + ".failed_checkout", value.getNumFailedCheckouts());
				map.put(PREFIX_KEY + SPLIT + dataSourceName + ".failed_test", value.getNumFailedIdleTests());
			}
		}

		return map;
	}

	private C3P0MonitorInfo getC3P0MonitorInfo(ObjectName objectName) {
		C3P0MonitorInfo c3P0MonitorInfo = new C3P0MonitorInfo();
		String jdbcUrl = getStringAttribute(objectName, "jdbcUrl");

		c3P0MonitorInfo.setJdbcUrl(jdbcUrl);
		c3P0MonitorInfo.setNumBusyConnections(getIntegerAttribute(objectName, "numBusyConnections", false));
		c3P0MonitorInfo.setNumConnections(getIntegerAttribute(objectName, "numConnections", false));
		c3P0MonitorInfo.setNumIdleConnections(getIntegerAttribute(objectName, "numIdleConnections", false));

		c3P0MonitorInfo.setNumFailedCheckins((getLongAttribute(objectName, "numFailedCheckinsDefaultUser", false)));
		c3P0MonitorInfo.setNumFailedCheckouts((getLongAttribute(objectName, "numFailedCheckoutsDefaultUser", false)));
		c3P0MonitorInfo.setNumFailedIdleTests((getLongAttribute(objectName, "numFailedIdleTestsDefaultUser", false)));

		return c3P0MonitorInfo;
	}

	private Map<String, C3P0MonitorInfo> getC3P0MonitorInfoMap() {
		Map<String, C3P0MonitorInfo> dataSourceInfoMap = new LinkedHashMap<String, C3P0MonitorInfo>();

		try {
			ObjectName pooledDataSourceObjectName = new ObjectName("com.mchange.v2.c3p0", "type", "PooledDataSource*");
			Set<ObjectName> objectNameSet = m_mbeanServer.queryNames(pooledDataSourceObjectName, null);

			if (objectNameSet == null || objectNameSet.size() == 0) {
				return dataSourceInfoMap;
			}

			Map<String, Integer> datasources = new LinkedHashMap<String, Integer>();

			for (ObjectName objectName : objectNameSet) {
				C3P0MonitorInfo info = getC3P0MonitorInfo(objectName);
				String url = info.getJdbcUrl();
				Database datasource = m_databaseParser.parseDatabase(url);
				String key = getConnction(datasources, datasource.toString());

				dataSourceInfoMap.put(key, info);
			}
		} catch (Exception e) {
		}
		return dataSourceInfoMap;
	}

	@Override
	public String getId() {
		return "datasource.c3p0";
	}

	@Override
	public Map<String, String> getProperties() {
		return convert(doCollect());
	}

}
