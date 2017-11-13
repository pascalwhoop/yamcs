package org.yamcs.yarch.rocksdb;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.yarch.ColumnDefinition;
import org.yamcs.yarch.DataType;
import org.yamcs.yarch.Partition;
import org.yamcs.yarch.PartitioningSpec;
import org.yamcs.yarch.TableDefinition;
import org.yamcs.yarch.TupleDefinition;
import org.yamcs.yarch.YarchDatabase;
import org.yamcs.yarch.YarchDatabaseInstance;
import org.yamcs.yarch.TableDefinition.PartitionStorage;

import com.google.common.io.Files;

import org.yamcs.utils.FileUtils;
import org.yamcs.utils.TimeEncoding;

import static org.junit.Assert.*;

public class PartitionManagerTest {

    @BeforeClass
    static public void init() {
	TimeEncoding.setUp();
    }

    TableDefinition getTableDefTimeAndValue() throws Exception {
	TupleDefinition tdef=new TupleDefinition();
	tdef.addColumn(new ColumnDefinition("gentime", DataType.TIMESTAMP));
	tdef.addColumn(new ColumnDefinition("packetid", DataType.INT));
	PartitioningSpec spec=PartitioningSpec.timeAndValueSpec("gentime", "packetid");
	spec.setTimePartitioningSchema("YYYY/DOY");

	TableDefinition tblDef = new TableDefinition("tbltest", tdef, Arrays.asList("gentime"));
	tblDef.setPartitioningSpec(spec);

	return tblDef;
    }
    
    TableDefinition getTableDefValue() throws Exception {
        TupleDefinition tdef=new TupleDefinition();
        tdef.addColumn(new ColumnDefinition("gentime", DataType.TIMESTAMP));
        tdef.addColumn(new ColumnDefinition("packetid", DataType.INT));
        PartitioningSpec spec=PartitioningSpec.valueSpec("packetid");

        TableDefinition tblDef = new TableDefinition("tbltest", tdef, Arrays.asList("gentime"));
        tblDef.setPartitioningSpec(spec);

        return tblDef;
    }
    
    
    TableDefinition getTableDefTime() throws Exception {
        TupleDefinition tdef=new TupleDefinition();
        tdef.addColumn(new ColumnDefinition("gentime", DataType.TIMESTAMP));
        tdef.addColumn(new ColumnDefinition("packetid", DataType.INT));
        PartitioningSpec spec=PartitioningSpec.timeSpec("gentime");
        spec.setTimePartitioningSchema("YYYY/DOY");

        TableDefinition tblDef = new TableDefinition("tbltest", tdef, Arrays.asList("gentime"));
        tblDef.setPartitioningSpec(spec);

        return tblDef;
    }

    @Test
    public void createAndIteratePartitions() throws Exception {
        Tablespace tablespace = new Tablespace("test", (byte)1);
        String tmpdir=Files.createTempDir().getAbsolutePath();
	TableDefinition tblDef= getTableDefTimeAndValue();

	RdbPartitionManager pm = new RdbPartitionManager(tablespace, YarchDatabase.getInstance("test"), tblDef);
	RdbPartition part= (RdbPartition) pm.createAndGetPartition(TimeEncoding.parse("2011-01-01T00:00:00"), 1);
	assertEquals("2011/001/tbltest", part.dir);

	part = (RdbPartition) pm.createAndGetPartition(TimeEncoding.parse("2011-03-01T00:00:00"), 1);
	assertEquals("2011/060/tbltest",part.dir);

	part = (RdbPartition) pm.createAndGetPartition(TimeEncoding.parse("2011-02-01T00:00:00"), 2);
	assertEquals("2011/032/tbltest",part.dir);

	part = (RdbPartition) pm.createAndGetPartition(TimeEncoding.parse("2011-02-01T00:00:00"), 3);
	assertEquals("2011/032/tbltest",part.dir);

	part = (RdbPartition) pm.createAndGetPartition(TimeEncoding.parse("2011-03-01T00:00:00"), 3);
	assertEquals("2011/060/tbltest", part.dir);

	Set<Object>filter=new HashSet<Object>();
	filter.add(1);
	filter.add(3);
	Iterator<List<Partition>> it=pm.iterator(TimeEncoding.parse("2011-02-01T00:00:00"), filter);
	assertTrue(it.hasNext());
	List<Partition> parts=it.next();
	assertEquals(1, parts.size());

	assertTrue(it.hasNext());
	parts=it.next();
	assertEquals("2011/060/tbltest", ((RdbPartition)parts.get(0)).dir);
	assertEquals("2011/060/tbltest", ((RdbPartition)parts.get(1)).dir);

	FileUtils.deleteRecursively(new File(tmpdir).toPath());
	assertTrue(false);//close and reopen the tablespace
    }    

}
