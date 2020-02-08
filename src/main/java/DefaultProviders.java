
import org.codejargon.feather.Provides;

import sam.config.AbstractConfig;
import sam.config.Config;
import sam.myutils.System2;
import sam.nopkg.EnsureSingleton;

class DefaultProviders {
	private static final EnsureSingleton SINGLETON = new EnsureSingleton();
	{ SINGLETON.init(); }
	
	public final Config config = new AbstractConfig() {
		
		@Override
		public boolean has(String key) {
			return opt(key, null) != null;
		}
		
		@Override
		protected Object opt(String key, Object defaultValue) {
			String s = System2.lookup(key);
			return s == null ? defaultValue : s;
		}
	}; 
	
	@Provides
	Config config() {
		return config;
	}
}
