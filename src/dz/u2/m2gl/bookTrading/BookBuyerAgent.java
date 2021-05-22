

package dz.u2.m2gl.bookTrading;

import jade.core.Agent;

import javax.swing.JOptionPane;

import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookBuyerAgent extends Agent {
	private String targetBookTitle;
	private AID[] sellerAgents;
	private AID[] BestSellerAgents;
	protected void setup() {
		System.out.println("Salam! Agent-achteur "+getAID().getName()+" est prét.");
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			targetBookTitle = (String) args[0];
			System.out.println("Le livre cible est  "+targetBookTitle);

			// Ajouter un TickerBehaviour qui planifie une demande aux agents vendeurs toutes les minutes
			addBehaviour(new TickerBehaviour(this, 15000) {
				protected void onTick() {
					System.out.println("Essayer d'acheter "+targetBookTitle);
					// Mettre à jour la liste des agents vendeurs
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("book-selling");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Nous avons trouvé les agents vendeurs suivants:");
						sellerAgents = new AID[result.length];
						 BestSellerAgents = new AID[3];

						for (int i = 0; i < result.length; ++i) {
							sellerAgents[i] = result[i].getName();
							System.out.println(sellerAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Effectuer la demande
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			// Faire résilier l'agent
			System.out.println("Aucun titre de livre cible spécifié");
			doDelete();
		}
	}

	// Placez les opérations de nettoyage des agents ici
	protected void takeDown() {
		// Imprimer un message de licenciement
		System.out.println("Agent-acheteur "+getAID().getName()+" terminer.");
	}

	
	private class RequestPerformer extends Behaviour {
		private AID bestSeller; // L'agent qui propose la meilleure offre
		private int bestPrice; 
		private ACLMessage reply2;
		private int nbestPrice; // Le meilleur prix offert
		private int repliesCnt = 0; //Le compteur de réponses des agents vendeurs
		private MessageTemplate mt;
		private MessageTemplate mt1;// Le modèle pour recevoir des réponses
		private int step = 0;
		int i=0;
		public void action() {
			switch (step) {
			case 0:
				//Envoyez le CFP à tous les vendeurs
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < sellerAgents.length; ++i) {
					cfp.addReceiver(sellerAgents[i]);
				} 
				cfp.setContent(targetBookTitle);
				cfp.setConversationId("book-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				System.out.println("Acheteur: Salam, je veux acheter un livre "+targetBookTitle+" l'avez vous?");

				myAgent.send(cfp);
				// Préparez le modèle pour obtenir des propositions
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Recevoir toutes les propositions / refus des agents vendeurs
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Réponse reçue
					
					

					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// Ceci est une offre
						int price = Integer.parseInt(reply.getContent());
						BestSellerAgents[i]=reply.getSender();
						System.out.println("---------------"+i+"----"+BestSellerAgents[i]);
						i++;
						if (bestSeller == null || price < bestPrice) {
							// C'est la meilleure offre actuellement
							bestPrice = price;
							bestSeller = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						// Nous avons reçu toutes les réponses
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:			
				// Envoyez le cfp à tous les vendeurs
				ACLMessage cfp1 = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < BestSellerAgents.length; ++i) {
					cfp1.addReceiver(BestSellerAgents[i]);
					System.out.println("++++++++"+i+"+++++"+BestSellerAgents[i]);

				} 
				//cfp1.addReceiver(bestSeller);
				cfp1.setContent("sold");
				cfp1.setConversationId("book-trade");
				cfp1.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				System.out.println("Acheteur: puis-je avoir une réduction sur le livre de "+targetBookTitle+"?");
				bestSeller= null;
				repliesCnt=0;
				myAgent.send(cfp1);
				// Préparez le modèle pour obtenir des propositions
				mt1 = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(cfp1.getReplyWith()));
				step = 3;
				break;
			case 3:
				// Recevoir toutes les propositions / refus des agents vendeurs
				reply2 = myAgent.receive(mt1);
				//System.out.println("+++++++reply2 buyer++"+reply2);
				if (reply2 != null) {
					// Réponse reçue�
					//System.out.println("+++++++reply2 != null++"+reply2.getSender());

					if (reply2.getPerformative() == ACLMessage.PROPOSE) {
						// Ceci est une offre
						int price = Integer.parseInt(reply2.getContent());
						//System.out.println("++++++++price+++++"+price);
						
						if (bestSeller == null || price < nbestPrice) {
							// C'est la meilleure offre actuellement
							nbestPrice = price;
							bestSeller = reply2.getSender();
							//System.out.println("+++++++bestSeller+++"+bestSeller);

						}
					}
					repliesCnt++;
					if (repliesCnt >= sellerAgents.length) {
						//Nous avons reçu toutes les réponses
						step = 4; 
					}
				}
				else {
					block();
				}
				break;
				
			case 4:
				// Envoyez le bon de commande au vendeur qui a fourni la meilleure offre
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(targetBookTitle);
				order.setConversationId("book-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Préparez le modèle pour obtenir la réponse au bon de commande
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 5;
				break;
			case 5:      
				//Recevoir la réponse au bon de commande
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Réponse au bon de commande reçue
					if (reply.getPerformative() == ACLMessage. INFORM) {
						// Achat réussi. Nous pouvons résilier
						System.out.println(targetBookTitle+" acheté avec succès auprès de l'agent "+reply.getSender().getName());
						System.out.println("Price = "+nbestPrice);
						JOptionPane.showMessageDialog(null, "acheté avec succès auprès de l'agent:"+reply.getSender().getName().split("@")[0]+" with price of "+nbestPrice);
						myAgent.doDelete();
					}
					else {
						System.out.println("Ooops meskine :( Échec de la tentative: le livre demandé est déjà vendu.");
					}

					step = 6;
				}
				else {
					block();
					
				}
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestSeller == null) {
				System.out.println("La tentative a échoué: "+targetBookTitle+" non disponible à la vente");
			}
			return ((step == 2 && bestSeller == null) || step == 6);
		}
	}  // Fin de la classe interne RequestPerformer
}
