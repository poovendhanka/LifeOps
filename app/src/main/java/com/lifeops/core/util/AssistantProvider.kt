package com.lifeops.core.util
import com.lifeops.core.database.ItemEntity
// A provider returns proposals, never executes mutations. The UI must approve each proposal.
interface AssistantProvider { suspend fun propose(query:String,snapshot:List<ItemEntity>):AssistantProposal }
data class AssistantProposal(val summary:String,val proposedChanges:List<ProposedChange>)
data class ProposedChange(val itemId:String?,val explanation:String,val proposedItem:ItemEntity?,val destructive:Boolean)
