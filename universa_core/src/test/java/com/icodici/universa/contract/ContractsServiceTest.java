/*
 * Copyright (c) 2017 Sergey Chernov, iCodici S.n.C, All Rights Reserved
 *
 * Written by Sergey Chernov <real.sergeych@gmail.com>, August 2017.
 *
 */

package com.icodici.universa.contract;

import com.icodici.crypto.PrivateKey;
import com.icodici.crypto.PublicKey;
import com.icodici.universa.Errors;
import com.icodici.universa.contract.roles.RoleLink;
import com.icodici.universa.contract.roles.SimpleRole;
import com.icodici.universa.node.network.TestKeys;
import net.sergeych.tools.Do;
import org.junit.Ignore;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ContractsServiceTest extends ContractTestBase {

    @Test
    public void badRevoke() throws Exception {
        Contract c = Contract.fromDslFile(rootPath + "simple_root_contract.yml");
        c.addSignerKeyFromFile(rootPath+"_xer0yfe2nn1xthc.private.unikey");
        c.seal();

        PrivateKey issuer = TestKeys.privateKey(2);
        Contract tc = c.createRevocation(issuer);

        // c can't be revoked with this key!
        boolean result = tc.check();
        assertFalse(result);
        assertEquals(1, tc.getErrors().size());
        assertEquals(Errors.FORBIDDEN, tc.getErrors().get(0).getError());
    }

    @Test
    public void goodRevoke() throws Exception {
        Contract c = Contract.fromDslFile(rootPath + "simple_root_contract.yml");
        c.addSignerKeyFromFile(rootPath+"_xer0yfe2nn1xthc.private.unikey");
        PrivateKey goodKey = c.getKeysToSignWith().iterator().next();
        // let's make this key among owners
        ((SimpleRole)c.getRole("owner")).addKeyRecord(new KeyRecord(goodKey.getPublicKey()));
        c.seal();

        Contract revokeContract = c.createRevocation(goodKey);


        assertTrue(revokeContract.check());
//        tc.traceErrors();
    }


    @Test
    public void checkTransactional() throws Exception {

        PrivateKey manufacturePrivateKey = new PrivateKey(Do.read(rootPath + "_xer0yfe2nn1xthc.private.unikey"));

        Contract delorean = Contract.fromDslFile(rootPath + "DeLoreanOwnership.yml");
        delorean.addSignerKey(manufacturePrivateKey);
        delorean.seal();
        delorean.traceErrors();

        Contract.Transactional transactional = delorean.createTransactionalSection();
        Reference reference = new Reference();
//        reference.setName("transactional_example");
        transactional.addReference(reference);
        Contract deloreanTransactional = delorean.createRevision(transactional);
        deloreanTransactional.addSignerKey(manufacturePrivateKey);
        deloreanTransactional.seal();
        deloreanTransactional.traceErrors();

    }

    private void checkCreateParcel(String contract_file_payload, String contract_file_payment) throws Exception
    {
        final String ROOT_PATH = "./src/test_contracts/contractService/";
        PrivateKey privateKey = TestKeys.privateKey(3);

        Contract payload = Contract.fromDslFile(ROOT_PATH + contract_file_payload);
        payload.addSignerKey(privateKey);
        payload.seal();

        Contract payment = Contract.fromDslFile(ROOT_PATH + contract_file_payment);
        payment.addSignerKey(privateKey);
        payment.seal();

        Set<PrivateKey> PrivateKeys = new HashSet<>();
        PrivateKeys.add(privateKey);

        Parcel parcel = ContractsService.createParcel(payload, payment, 20, PrivateKeys);
    }

    @Test
    public void checkCreateGoodParcel() throws Exception
    {
        checkCreateParcel("simple_root_contract.yml", "simple_root_contract.yml");
    }

    @Ignore
    @Test
    public void checkCreateParcelBadPayload() throws Exception
    {
        checkCreateParcel("bad_contract_payload.yml", "simple_root_contract.yml");
    }

    @Ignore
    @Test
    public void checkCreateParcelBadPayment() throws Exception
    {
        checkCreateParcel("simple_root_contract.yml","bad_contract_payment.yml");
    }
}