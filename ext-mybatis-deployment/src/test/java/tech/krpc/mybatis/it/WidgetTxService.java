package tech.krpc.mybatis.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/** Exercises the JTA-transactional write path for the tx-safety regression. */
@ApplicationScoped
public class WidgetTxService {

    @Inject
    WidgetMapper mapper;

    /** Two writes in one JTA transaction; both must be visible after commit. */
    @Transactional
    public void saveTwice(int id1, String name1, int id2, String name2) {
        mapper.remove(id1);
        mapper.remove(id2);
        mapper.save(id1, name1);
        mapper.save(id2, name2);
    }

    /** Write then throw; the row must NOT persist (rollback). */
    @Transactional
    public void saveThenThrow(int id, String name) {
        mapper.remove(id);
        mapper.save(id, name);
        throw new IllegalStateException("intentional rollback trigger");
    }
}
