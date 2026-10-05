package org.brahypno.maledict.common.entity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RavenTrustTest {
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void chickTrustsBothPlayersWhoFedItsParents() {
        assertEquals(List.of(A, B), RavenTrust.fromBreeding(A, B));
    }

    @Test
    void theSamePlayerFeedingBothParentsMakesOneTrustEntry() {
        assertEquals(List.of(A), RavenTrust.fromBreeding(A, A));
    }

    @Test
    void unfedParentsAddNoTrustedPlayer() {
        assertEquals(List.of(), RavenTrust.fromBreeding(null, null));
        assertEquals(List.of(B), RavenTrust.fromBreeding(null, B));
    }
}
