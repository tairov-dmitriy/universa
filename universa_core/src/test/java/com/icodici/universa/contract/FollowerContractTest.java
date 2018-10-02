package com.icodici.universa.contract;

import com.icodici.crypto.KeyAddress;
import com.icodici.crypto.PrivateKey;
import com.icodici.crypto.PublicKey;
import com.icodici.universa.contract.permissions.ModifyDataPermission;
import com.icodici.universa.contract.permissions.Permission;
import com.icodici.universa.contract.services.FollowerContract;
import com.icodici.universa.contract.services.NSmartContract;
import com.icodici.universa.contract.services.SlotContract;
import com.icodici.universa.node2.Config;
import net.sergeych.biserializer.BossBiMapper;
import net.sergeych.biserializer.DefaultBiMapper;
import net.sergeych.collections.Multimap;
import net.sergeych.tools.Binder;
import net.sergeych.tools.Do;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class FollowerContractTest extends ContractTestBase {

    static Config nodeConfig;

    @BeforeClass
    public static void beforeClass() throws Exception {
        nodeConfig = new Config();
        nodeConfig.addTransactionUnitsIssuerKeyData(new KeyAddress("Zau3tT8YtDkj3UDBSznrWHAjbhhU4SXsfQLWDFsv5vw24TLn6s"));
    }

    @Test
    public void goodFollowerContract() throws Exception {

        final PrivateKey key = new PrivateKey(Do.read(rootPath + "_xer0yfe2nn1xthc.private.unikey"));
        final PrivateKey key2 = new PrivateKey(Do.read(rootPath + "test_network_whitekey.private.unikey"));

        Contract simpleContract = new Contract(key2);
        simpleContract.seal();
        simpleContract.check();
        simpleContract.traceErrors();
        assertTrue(simpleContract.isOk());

        PrivateKey privateKey = new PrivateKey(2048);
        PublicKey callbackKey = privateKey.getPublicKey();

        Contract smartContract = new FollowerContract(key);

        assertTrue(smartContract instanceof FollowerContract);

        ((FollowerContract)smartContract).setNodeInfoProvider(nodeInfoProvider);
        ((FollowerContract)smartContract).putTrackingOrigin(simpleContract.getOrigin(), "http:\\\\localhost:7777\\follow.callback", callbackKey);

        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), smartContract.getDefinition().getExtendedType());
        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), smartContract.get("definition.extended_type"));

        Multimap<String, Permission> permissions = smartContract.getPermissions();
        Collection<Permission> mdp = permissions.get("modify_data");
        assertNotNull(mdp);
        assertTrue(((ModifyDataPermission)mdp.iterator().next()).getFields().containsKey("action"));

        assertEquals(((FollowerContract) smartContract).getCallbackKeys().get("http:\\\\localhost:7777\\follow.callback"),callbackKey );
        assertEquals(((FollowerContract) smartContract).getTrackingOrigins().get(simpleContract.getOrigin()),
                "http:\\\\localhost:7777\\follow.callback");
        assertTrue(((FollowerContract) smartContract).isOriginTracking(simpleContract.getOrigin()));
        assertTrue(((FollowerContract) smartContract).isCallbackURLUsed("http:\\\\localhost:7777\\follow.callback"));

    }

    @Test
    public void goodSmartContractFromDSL() throws Exception {

        final PrivateKey key2 = new PrivateKey(Do.read(rootPath + "test_network_whitekey.private.unikey"));

        Contract simpleContract = new Contract(key2);
        simpleContract.seal();
        simpleContract.check();
        simpleContract.traceErrors();
        assertTrue(simpleContract.isOk());

        PrivateKey privateKey = new PrivateKey(2048);
        PublicKey callbackKey = privateKey.getPublicKey();

        //Contract smartContract = new FollowerContract(key);
        Contract smartContract = FollowerContract.fromDslFile(rootPath + "FollowerDSLTemplate.yml");
        smartContract.addSignerKeyFromFile(rootPath + "_xer0yfe2nn1xthc.private.unikey");

        assertTrue(smartContract instanceof FollowerContract);

        ((FollowerContract)smartContract).setNodeInfoProvider(nodeInfoProvider);
        ((FollowerContract)smartContract).putTrackingOrigin(simpleContract.getOrigin(), "http:\\\\localhost:7777\\follow.callback", callbackKey);

        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), smartContract.getDefinition().getExtendedType());
        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), smartContract.get("definition.extended_type"));

        Multimap<String, Permission> permissions = smartContract.getPermissions();
        Collection<Permission> mdp = permissions.get("modify_data");
        assertNotNull(mdp);
        assertTrue(((ModifyDataPermission)mdp.iterator().next()).getFields().containsKey("action"));

        assertEquals(((FollowerContract) smartContract).getCallbackKeys().get("http:\\\\localhost:7777\\follow.callback"),callbackKey );
        assertEquals(((FollowerContract) smartContract).getTrackingOrigins().get(simpleContract.getOrigin()),
                "http:\\\\localhost:7777\\follow.callback");
        assertTrue(((FollowerContract) smartContract).isOriginTracking(simpleContract.getOrigin()));
        assertTrue(((FollowerContract) smartContract).isCallbackURLUsed("http:\\\\localhost:7777\\follow.callback"));

    }

    private NSmartContract.NodeInfoProvider nodeInfoProvider = new NSmartContract.NodeInfoProvider() {

        Config config = new Config();
        @Override
        public Set<KeyAddress> getUIssuerKeys() {
            return config.getUIssuerKeys();
        }

        @Override
        public String getUIssuerName() {
            return config.getUIssuerName();
        }

        @Override
        public int getMinPayment(String extendedType) {
            return config.getMinPayment(extendedType);
        }

        @Override
        public double getRate(String extendedType) {
            return config.getRate(extendedType);
        }

        @Override
        public Collection<PublicKey> getAdditionalKeysToSignWith(String extendedType) {
            Set<PublicKey> set = new HashSet<>();
            if(extendedType.equals(NSmartContract.SmartContractType.UNS1)) {
                set.add(config.getAuthorizedNameServiceCenterKey());
            }
            return set;
        }
    };

    @Test
    public void serializeSmartContract() throws Exception {
        final PrivateKey key = new PrivateKey(Do.read(rootPath + "_xer0yfe2nn1xthc.private.unikey"));
        final PrivateKey key2 = new PrivateKey(Do.read(rootPath + "test_network_whitekey.private.unikey"));

        Contract simpleContract = new Contract(key2);
        simpleContract.seal();
        simpleContract.check();
        simpleContract.traceErrors();
        assertTrue(simpleContract.isOk());

        PrivateKey privateKey = new PrivateKey(2048);
        PublicKey callbackKey = privateKey.getPublicKey();

        Contract smartContract = new FollowerContract(key);

        assertTrue(smartContract instanceof FollowerContract);

        ((FollowerContract)smartContract).setNodeInfoProvider(nodeInfoProvider);

        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        Binder b = BossBiMapper.serialize(smartContract);

        Contract desContract = DefaultBiMapper.deserialize(b);
        assertSameContracts(smartContract, desContract);

        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), desContract.getDefinition().getExtendedType());
        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), desContract.get("definition.extended_type"));

        Multimap<String, Permission> permissions = desContract.getPermissions();
        Collection<Permission> mdp = permissions.get("modify_data");
        assertNotNull(mdp);
        assertTrue(((ModifyDataPermission)mdp.iterator().next()).getFields().containsKey("action"));

        ((FollowerContract)desContract).putTrackingOrigin(simpleContract.getOrigin(), "http:\\\\localhost:7777\\follow.callback", callbackKey);

        assertEquals(((FollowerContract) desContract).getCallbackKeys().get("http:\\\\localhost:7777\\follow.callback"),callbackKey );
        assertEquals(((FollowerContract) desContract).getTrackingOrigins().get(simpleContract.getOrigin()),
                "http:\\\\localhost:7777\\follow.callback");
        assertTrue(((FollowerContract) desContract).isOriginTracking(simpleContract.getOrigin()));
        assertTrue(((FollowerContract) desContract).isCallbackURLUsed("http:\\\\localhost:7777\\follow.callback"));

        Contract copiedContract = smartContract.copy();
        assertSameContracts(smartContract, copiedContract);

        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), copiedContract.getDefinition().getExtendedType());
        assertEquals(NSmartContract.SmartContractType.FOLLOWER1.name(), copiedContract.get("definition.extended_type"));

        assertTrue(copiedContract instanceof FollowerContract);

        permissions = desContract.getPermissions();
        mdp = permissions.get("modify_data");
        assertNotNull(mdp);
        assertTrue(((ModifyDataPermission)mdp.iterator().next()).getFields().containsKey("action"));

        ((FollowerContract)copiedContract).putTrackingOrigin(simpleContract.getOrigin(), "http:\\\\localhost:7777\\follow.callback", callbackKey);

        assertEquals(((FollowerContract) copiedContract).getCallbackKeys().get("http:\\\\localhost:7777\\follow.callback"),callbackKey );
        assertEquals(((FollowerContract) copiedContract).getTrackingOrigins().get(simpleContract.getOrigin()),
                "http:\\\\localhost:7777\\follow.callback");
        assertTrue(((FollowerContract) copiedContract).isOriginTracking(simpleContract.getOrigin()));
        assertTrue(((FollowerContract) copiedContract).isCallbackURLUsed("http:\\\\localhost:7777\\follow.callback"));

   }

    @Test
    public void keepRevisions() throws Exception {

        final PrivateKey key = new PrivateKey(Do.read(rootPath + "_xer0yfe2nn1xthc.private.unikey"));

        Contract simpleContract = new Contract(key);
        simpleContract.seal();

        Contract paymentDecreased = createSlotPayment();

        Contract smartContract = new SlotContract(key);

        assertTrue(smartContract instanceof SlotContract);

        ((SlotContract)smartContract).putTrackingContract(simpleContract);
        ((SlotContract)smartContract).setNodeInfoProvider(nodeInfoProvider);
        ((SlotContract)smartContract).setKeepRevisions(2);
        smartContract.addNewItems(paymentDecreased);
        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        assertEquals(1, ((SlotContract)smartContract).getTrackingContracts().size());
        assertEquals(simpleContract.getId(), ((SlotContract) smartContract).getTrackingContract().getId());
        assertEquals(simpleContract.getId(), TransactionPack.unpack(((SlotContract) smartContract).getPackedTrackingContract()).getContract().getId());

        Binder trackingHashesAsBase64 = smartContract.getStateData().getBinder("tracking_contract");
        for (String k : trackingHashesAsBase64.keySet()) {
            byte[] packed = trackingHashesAsBase64.getBinary(k);
            if (packed != null) {
                Contract c = Contract.fromPackedTransaction(packed);
                assertEquals(simpleContract.getId(), c.getId());
            }
        }

        Contract simpleContract2 = simpleContract.createRevision(key);
        simpleContract2.seal();
        ((SlotContract)smartContract).putTrackingContract(simpleContract2);
        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        assertEquals(2, ((SlotContract)smartContract).getTrackingContracts().size());
        assertEquals(simpleContract2.getId(), ((SlotContract) smartContract).getTrackingContract().getId());
        assertEquals(simpleContract2.getId(), TransactionPack.unpack(((SlotContract) smartContract).getPackedTrackingContract()).getContract().getId());

        trackingHashesAsBase64 = smartContract.getStateData().getBinder("tracking_contract");
        for (String k : trackingHashesAsBase64.keySet()) {
            byte[] packed = trackingHashesAsBase64.getBinary(k);
            if (packed != null) {
                Contract c = Contract.fromPackedTransaction(packed);
                assertThat(c.getId(), Matchers.anyOf(equalTo(simpleContract.getId()), equalTo(simpleContract2.getId())));
            }
        }

        Contract simpleContract3 = simpleContract2.createRevision(key);
        simpleContract3.seal();
        ((SlotContract)smartContract).putTrackingContract(simpleContract3);
        smartContract.seal();
        smartContract.check();
        smartContract.traceErrors();
        assertTrue(smartContract.isOk());

        assertEquals(2, ((SlotContract)smartContract).getTrackingContracts().size());
        assertEquals(simpleContract3.getId(), ((SlotContract) smartContract).getTrackingContract().getId());
        assertEquals(simpleContract3.getId(), TransactionPack.unpack(((SlotContract) smartContract).getPackedTrackingContract()).getContract().getId());

        trackingHashesAsBase64 = smartContract.getStateData().getBinder("tracking_contract");
        for (String k : trackingHashesAsBase64.keySet()) {
            byte[] packed = trackingHashesAsBase64.getBinary(k);
            if (packed != null) {
                Contract c = Contract.fromPackedTransaction(packed);
                assertThat(c.getId(), Matchers.anyOf(
                        equalTo(simpleContract.getId()),
                        equalTo(simpleContract2.getId()),
                        equalTo(simpleContract3.getId())
                ));
            }
        }
    }

    public Contract createSlotPayment() throws IOException {

        PrivateKey ownerKey = new PrivateKey(Do.read(rootPath + "keys/stepan_mamontov.private.unikey"));
        Set<PublicKey> keys = new HashSet();
        keys.add(ownerKey.getPublicKey());
        Contract stepaU = InnerContractsService.createFreshU(100000000, keys);
        Contract paymentDecreased = stepaU.createRevision(ownerKey);
        paymentDecreased.getStateData().set("transaction_units", stepaU.getStateData().getIntOrThrow("transaction_units") - 100);
        paymentDecreased.seal();

        return paymentDecreased;
    }
}
