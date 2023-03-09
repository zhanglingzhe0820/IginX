package cn.edu.tsinghua.iginx.compaction;

import cn.edu.tsinghua.iginx.conf.ConfigDescriptor;
import cn.edu.tsinghua.iginx.engine.physical.PhysicalEngine;
import cn.edu.tsinghua.iginx.engine.physical.exception.PhysicalException;
import cn.edu.tsinghua.iginx.engine.shared.TimeRange;
import cn.edu.tsinghua.iginx.engine.shared.operator.Delete;
import cn.edu.tsinghua.iginx.engine.shared.source.FragmentSource;
import cn.edu.tsinghua.iginx.metadata.DefaultMetaManager;
import cn.edu.tsinghua.iginx.metadata.IMetaManager;
import cn.edu.tsinghua.iginx.metadata.entity.FragmentMeta;
import cn.edu.tsinghua.iginx.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class FragmentDeletionCompaction extends Compaction {

    private static final Logger logger = LoggerFactory.getLogger(FragmentDeletionCompaction.class);
    private List<FragmentMeta> toDeletionFragments = new ArrayList<>();

    public FragmentDeletionCompaction(PhysicalEngine physicalEngine, IMetaManager metaManager) {
        super(physicalEngine, metaManager);
    }

    @Override
    public boolean needCompaction() throws Exception {
        //集中信息（初版主要是统计分区热度）
        Pair<Map<FragmentMeta, Long>, Map<FragmentMeta, Long>> fragmentHeatPair = metaManager
            .loadFragmentHeat();
        Map<FragmentMeta, Long> fragmentHeatWriteMap = fragmentHeatPair.getK();
        Map<FragmentMeta, Long> fragmentHeatReadMap = fragmentHeatPair.getV();
        if (fragmentHeatWriteMap == null) {
            fragmentHeatWriteMap = new HashMap<>();
        }
        if (fragmentHeatReadMap == null) {
            fragmentHeatReadMap = new HashMap<>();
        }

        long totalHeats = 0;
        for (Map.Entry<FragmentMeta, Long> fragmentHeatReadEntry : fragmentHeatReadMap.entrySet()) {
            totalHeats += fragmentHeatReadEntry.getValue();
        }
        double limitReadHeats = totalHeats * 1.0 / fragmentHeatReadMap.size() * ConfigDescriptor.getInstance().getConfig().getFragmentCompactionReadRatioThreshold();

        // 判断是否要删除可定制化副本生成的冗余分片
        Map<FragmentMeta, List<FragmentMeta>> custFragmentMetaListMap = DefaultMetaManager.getInstance().getAllCustomizableReplicaFragmentList();
        for (Map.Entry<FragmentMeta, List<FragmentMeta>> custFragmentMetaListEntry : custFragmentMetaListMap.entrySet()) {
            long custHeats = 0L;
            for (FragmentMeta fragmentMeta : custFragmentMetaListEntry.getValue()) {
                for (Map.Entry<FragmentMeta, Long> fragmentHeatReadEntry : fragmentHeatReadMap.entrySet()) {
                    if (fragmentMeta.equals(fragmentHeatReadEntry.getKey())) {
                        custHeats += fragmentHeatReadEntry.getValue();
                    }
                }
            }
            if (limitReadHeats > custHeats) {
                logger.error("hit FragmentDeletionCompaction = {}", custFragmentMetaListEntry.getKey());
                toDeletionFragments.add(custFragmentMetaListEntry.getKey());
            }
        }

        return !toDeletionFragments.isEmpty();
    }

    @Override
    public void compact() throws PhysicalException {
        for (FragmentMeta fragmentMeta : toDeletionFragments) {
            // 删除可定制化副本分片元数据
            metaManager.removeCustomizableReplicaFragmentMeta(fragmentMeta);

            // 删除节点数据
//            List<String> paths = new ArrayList<>();
//            paths.add(fragmentMeta.getMasterStorageUnitId() + "*");
//            List<TimeRange> timeRanges = new ArrayList<>();
//            timeRanges.add(new TimeRange(fragmentMeta.getTimeInterval().getStartTime(), true,
//                fragmentMeta.getTimeInterval().getEndTime(), false));
//            Delete delete = new Delete(new FragmentSource(fragmentMeta), timeRanges, paths, null);
//            physicalEngine.execute(delete);
        }
    }
}
