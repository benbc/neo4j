/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.transaction.log;

class TransactionCommitment implements Commitment
{
    private final boolean hasLegacyIndexChanges;
    private final long transactionId;
    private final long transactionChecksum;
    private final LogPosition logPosition;
    private final TransactionIdStore transactionIdStore;
    private boolean markedAsCommitted;

    TransactionCommitment( boolean hasLegacyIndexChanges, long transactionId, long transactionChecksum,
            LogPosition logPosition, TransactionIdStore transactionIdStore )
    {
        this.hasLegacyIndexChanges = hasLegacyIndexChanges;
        this.transactionId = transactionId;
        this.transactionChecksum = transactionChecksum;
        this.logPosition = logPosition;
        this.transactionIdStore = transactionIdStore;
    }

    @Override
    public void publishAsCommitted()
    {
        markedAsCommitted = true;
        transactionIdStore.transactionCommitted( transactionId, transactionChecksum );
    }

    @Override
    public void publishAsClosed()
    {
        transactionIdStore.transactionClosed( transactionId,
                logPosition.getLogVersion(), logPosition.getByteOffset() );
    }

    @Override
    public boolean markedAsCommitted()
    {
        return markedAsCommitted;
    }

    @Override
    public boolean hasLegacyIndexChanges()
    {
        return hasLegacyIndexChanges;
    }
}
