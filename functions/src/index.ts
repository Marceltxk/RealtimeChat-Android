import {onDocumentCreated} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import {logger} from "firebase-functions";


admin.initializeApp();

export const notifyonnewmessage = onDocumentCreated(
  "conversations/{conversationId}/messages/{messageId}",
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) {
      logger.log("Nenhum dado associado ao evento.");
      return;
    }

    const message = snapshot.data();
    const senderId = message.senderId;
    const conversationId = event.params.conversationId;
    const messageId = event.params.messageId;

    const conversationRef = admin.firestore()
      .collection("conversations").doc(conversationId);
    const conversationDoc = await conversationRef.get();
    if (!conversationDoc.exists) {
      return;
    }

    const conversationData = conversationDoc.data();
    const isGroup = conversationData?.isGroup;

    const senderRef = admin.firestore().collection("users").doc(senderId);
    const senderDoc = await senderRef.get();
    const senderName = senderDoc.data()?.username || "Alguém";

    if (isGroup) {
      // --- LÓGICA PARA GRUPOS ---
      const groupName = conversationData?.groupName || "Um grupo";
      const participants = conversationData?.participants || [];
      const recipients = participants.filter((p: string) => p !== senderId);

      if (recipients.length === 0) {
        return;
      }

      const userDocs = await Promise.all(
        recipients.map((id: string) => admin
          .firestore().collection("users").doc(id).get())
      );
      const fcmTokens = userDocs.map((doc) => doc
        .data()?.fcmToken).filter((token) => token);

      if (fcmTokens.length > 0) {
        const payload = {
          notification: {
            title: groupName,
            body: `${senderName}: ${message.text}`,
          },
          data: {
            conversationId: conversationId,
            messageId: messageId,
          },
          tokens: fcmTokens,
        };

        try {
          const response = await admin.messaging().sendMulticast(payload);
          logger
            .log(`Notificação de grupo para ${fcmTokens.length} tokens.`,
              {response});
          if (response.failureCount > 0) {
            logger
              .warn(`Falha ao enviar para ${response.failureCount} tokens.`);
          }
        } catch (error) {
          logger.error("Erro ao executar sendMulticast:", error);
        }
      } else {
        logger
          .warn("Nenhum token FCM válido para os destinatários do grupo.");
      }
    } else {
      // --- LÓGICA PARA CHATS INDIVIDUAIS ---
      const participants = conversationData?.participants || [];
      const recipientId = participants.find((p: string) => p !== senderId);
      if (!recipientId) {
        return;
      }

      const userRef = admin.firestore().collection("users").doc(recipientId);
      const userDoc = await userRef.get();
      const fcmToken = userDoc.data()?.fcmToken;

      if (fcmToken) {
        const payload = {
          notification: {
            title: senderName,
            body: message.text},
          data: {
            conversationId: conversationId,
            messageId: messageId},
          token: fcmToken,
        };
        await admin.messaging().send(payload);
        logger.log(`Notificação individual enviada para ${recipientId}.`);
      }
    }

    // Lógica de "Entregue"
    const messageRef = snapshot.ref;
    const currentMessageDoc = await messageRef.get();
    if (currentMessageDoc.exists && currentMessageDoc
      .data()?.status !== "READ") {
      await messageRef.update({status: "DELIVERED"});
      logger.log(`Status da mensagem ${messageId} atualizado para DELIVERED.`);
    }
  }
);
