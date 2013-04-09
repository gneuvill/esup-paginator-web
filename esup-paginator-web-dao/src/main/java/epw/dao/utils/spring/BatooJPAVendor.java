package epw.dao.utils.spring;

import org.batoo.jpa.core.BatooPersistenceProvider;
import org.springframework.orm.jpa.JpaDialect;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;

import javax.persistence.spi.PersistenceProvider;
import java.util.HashMap;
import java.util.Map;

public class BatooJPAVendor extends AbstractJpaVendorAdapter{

  private final PersistenceProvider persistenceProvider = new BatooPersistenceProvider();
  private final BatooJpaDialect jpaDialect = new BatooJpaDialect();

  @Override
  public JpaDialect getJpaDialect() {
    return this.jpaDialect;
  }

  @Override
  public Map<String, Object> getJpaPropertyMap() {
    final Map<String, Object> jpaProperties = new HashMap<>();

    if (this.isGenerateDdl()) {
      jpaProperties.put("org.batoo.jpa.ddl", "UPDATE");
    }

    if (this.isShowSql()) {
      jpaProperties.put("org.batoo.jpa.sql_logging", "STDOUT");
    }

    return jpaProperties;
  }

  @Override
  public PersistenceProvider getPersistenceProvider() {
    return this.persistenceProvider;
  }

  @Override
  public String getPersistenceProviderRootPackage() {
    return "org.batoo.jpa";
  }
}
