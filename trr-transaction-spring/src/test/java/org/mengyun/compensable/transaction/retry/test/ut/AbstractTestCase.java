package org.mengyun.compensable.transaction.retry.test.ut;

import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Created by changmingxie on 12/2/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/trr-transaction-unit-test.xml","classpath:/trr-transaction.xml"})
public abstract class AbstractTestCase {

}