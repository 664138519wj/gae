package org.fh.gae.query.index.loader;

import lombok.extern.slf4j.Slf4j;
import org.fh.gae.config.GaeIndexProps;
import org.fh.gae.query.index.GaeIndex;
import org.fh.gae.query.index.auth.AuthIndex;
import org.fh.gae.query.index.idea.IdeaAuditIndex;
import org.fh.gae.query.index.idea.IdeaIndex;
import org.fh.gae.query.index.idea.UnitIdeaRelIndex;
import org.fh.gae.query.index.plan.PlanIndex;
import org.fh.gae.query.index.region.RegionIndex;
import org.fh.gae.query.index.tag.TagIndex;
import org.fh.gae.query.index.unit.AdUnitIndex;
import org.fh.gae.query.index.user.UserIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 全量索引加载器
 */
@Component
@Slf4j
public class IndexLoader {
    private static final String TOKEN_SEPARATOR = "\t";

    @Autowired
    private AuthIndex authIndex;
    @Autowired
    private UserIndex userIndex;
    @Autowired
    private PlanIndex planIndex;
    @Autowired
    private AdUnitIndex unitIndex;
    @Autowired
    private TagIndex tagIndex;
    @Autowired
    private IdeaIndex ideaIndex;
    @Autowired
    private UnitIdeaRelIndex unitIdeaRelIndex;
    @Autowired
    private IdeaAuditIndex ideaAuditIndex;
    @Autowired
    private RegionIndex regionIndex;

    @Autowired
    private GaeIndexProps indexProps;

    private Map<Integer, GaeIndex> idxMap;

    @PostConstruct
    public void init() throws IOException {
        idxMap = new HashMap<>();
        idxMap.put(AuthIndex.LEVEL, authIndex);
        idxMap.put(UserIndex.LEVEL, userIndex);
        idxMap.put(PlanIndex.LEVEL, planIndex);
        idxMap.put(AdUnitIndex.LEVEL, unitIndex);
        idxMap.put(TagIndex.LEVEL, tagIndex);
        idxMap.put(IdeaIndex.LEVEL, ideaIndex);
        idxMap.put(UnitIdeaRelIndex.LEVEL, unitIdeaRelIndex);
        idxMap.put(IdeaAuditIndex.LEVEL, ideaAuditIndex);
        idxMap.put(RegionIndex.LEVEL, regionIndex);

        loadIndex();
    }

    private void loadIndex() throws IOException {
        try (FileInputStream fis = new FileInputStream(indexProps.getPath() + File.separator + indexProps.getName())) {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(fis)
            );

            int okTot = 0;
            int errTot = 0;
            String line;
            while ( (line = reader.readLine()) != null ) {
                // 跳过空行
                if (0 == line.length()) {
                    // log.info("skip empty line");
                    continue;
                }

                if (line.startsWith("#")) {
                    // log.info("skip comment line");
                    continue;
                }

                // log.info("{}", line);
                int result = processLine(line);
                if (0 == result) {
                    ++okTot;
                } else {
                    ++errTot;
                }
            }

            log.info("index loaded, ok = {}, err = {}", okTot, errTot);

        } catch (IOException e) {
            throw e;
        }
    }

    public int processLine(String line) {
        try {

            String[] tokens = line.split(TOKEN_SEPARATOR);

            // 取出操作类型
            Integer op = Integer.valueOf(tokens[1]);
            // 取出层级
            Integer level = Integer.valueOf(tokens[0]);
            GaeIndex index = idxMap.get(level);

            if (tokens.length != index.getLength() + 2) {
                log.error("invalid index line: {}", line);
                return -1;
            }

            Object info = index.packageInfo(tokens);
            switch (op) {
                case 0:
                    index.add(info);
                    break;
                case 1:
                    index.update(info);
                    break;
                case 2:
                    index.delete(info);
                    break;

                default:
                    log.error("invalid index line(wrong op type): {}", line);
                    return -1;
            }

        } catch (Exception e) {
            log.error("invalid index line: {}", line);
            return -1;
        }

        return 0;
    }
}
