package cn.edu.tsinghua.iginx.compaction;

import cn.edu.tsinghua.iginx.conf.ConfigDescriptor;
import cn.edu.tsinghua.iginx.engine.physical.PhysicalEngine;
import cn.edu.tsinghua.iginx.engine.physical.exception.PhysicalException;
import cn.edu.tsinghua.iginx.metadata.IMetaManager;
import cn.edu.tsinghua.iginx.metadata.entity.FragmentMeta;
import cn.edu.tsinghua.iginx.metadata.entity.StorageUnitMeta;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LowAccessFragmentCompactionTest {

    private List<FragmentMeta> fragmentMetaSet = new ArrayList<>();
    private Map<FragmentMeta, Long> fragmentHeatWriteMap = new HashMap<>();
    private Map<FragmentMeta, Long> fragmentHeatReadMap = new HashMap<>();
    private Map<FragmentMeta, Long> fragmentMetaPointsMap = new HashMap<>();
    private PhysicalEngine physicalEngine = new PhysicalEngineMock();
    private IMetaManager metaManager = new MetaManagerMock();
    private LowAccessFragmentCompaction compaction = new LowAccessFragmentCompaction(physicalEngine, metaManager);

    @Before
    public void setUp() {
        StorageUnitMeta storageUnitMeta1 = new StorageUnitMeta("1", 1);
        StorageUnitMeta storageUnitMeta2 = new StorageUnitMeta("2", 2);
        FragmentMeta fragmentMeta1 = new FragmentMeta("root.a.b", "root.z", 0L, 1000L, storageUnitMeta1);
        FragmentMeta fragmentMeta2 = new FragmentMeta("root.z", "root.z.a", 0L, 1000L, storageUnitMeta1);
        FragmentMeta fragmentMeta3 = new FragmentMeta("root.z.a", "root.z.z", 0L, 1000L, storageUnitMeta1);
        FragmentMeta fragmentMeta4 = new FragmentMeta("root.z.z", null, 0L, 1000L, storageUnitMeta2);
        fragmentMetaSet.add(fragmentMeta1);
        fragmentMetaSet.add(fragmentMeta2);
        fragmentMetaSet.add(fragmentMeta3);
        fragmentMetaSet.add(fragmentMeta4);
        fragmentHeatWriteMap.put(fragmentMeta1, 100L);
        fragmentHeatWriteMap.put(fragmentMeta2, 100L);
        fragmentHeatReadMap.put(fragmentMeta3, 100L);
        fragmentHeatReadMap.put(fragmentMeta4, 100L);
        fragmentMetaPointsMap.put(fragmentMeta1, 100L);
        fragmentMetaPointsMap.put(fragmentMeta1, 100L);
        fragmentMetaPointsMap.put(fragmentMeta1, 50L);
        fragmentMetaPointsMap.put(fragmentMeta1, 100L);

        ConfigDescriptor.getInstance().getConfig().setFragmentCompactionReadThreshold(100);
        ConfigDescriptor.getInstance().getConfig().setFragmentCompactionWriteThreshold(100);
    }

    @Test
    public void testFragmentSelection() {
        List<List<FragmentMeta>> toCompactFragmentGroups = compaction.judgeCompaction(fragmentMetaSet, fragmentHeatWriteMap, fragmentHeatReadMap);
        assertEquals(toCompactFragmentGroups.size(), 1);
        assertEquals(toCompactFragmentGroups.get(0).size(), 2);
    }

    @Test
    public void testFragmentCompact() throws PhysicalException {
        List<List<FragmentMeta>> toCompactFragmentGroups = compaction.judgeCompaction(fragmentMetaSet, fragmentHeatWriteMap, fragmentHeatReadMap);
        compaction.executeCompaction(toCompactFragmentGroups, fragmentMetaPointsMap);
        List<FragmentMeta> fragmentMetas = metaManager.getFragments();
        assertEquals(fragmentMetas.size(), 1);
        assertEquals(fragmentMetas.get(0).getTsInterval().getStartTimeSeries(), "root.z.a");
        assertNull(fragmentMetas.get(0).getTsInterval().getEndTimeSeries());
        assertEquals(fragmentMetas.get(0).getMasterStorageUnit().getStorageEngineId(), 1);
        assertEquals(fragmentMetas.get(0).getTimeInterval().getStartTime(), 0);
        assertEquals(fragmentMetas.get(0).getTimeInterval().getEndTime(), 1000);
    }
}
