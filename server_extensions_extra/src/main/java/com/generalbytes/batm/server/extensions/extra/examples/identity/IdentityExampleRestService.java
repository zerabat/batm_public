/*************************************************************************************
 * Copyright (C) 2014-2020 GENERAL BYTES s.r.o. All rights reserved.
 *
 * This software may be distributed and modified under the terms of the GNU
 * General Public License version 2 (GPL2) as published by the Free Software
 * Foundation and appearing in the file GPL2.TXT included in the packaging of
 * this file. Please note that GPL2 Section 2[b] requires that all works based
 * on this software must also be made publicly available under the terms of
 * the GPL2 ("Copyleft").
 *
 * Contact information
 * -------------------
 *
 * GENERAL BYTES s.r.o.
 * Web      :  http://www.generalbytes.com
 *
 ************************************************************************************/
package com.generalbytes.batm.server.extensions.extra.examples.identity;

import com.generalbytes.batm.server.extensions.IExtensionContext;
import com.generalbytes.batm.server.extensions.IIdentity;
import com.generalbytes.batm.server.extensions.IIdentityNote;
import com.generalbytes.batm.server.extensions.IIdentityPiece;
import com.generalbytes.batm.server.extensions.ILimit;
import com.generalbytes.batm.server.extensions.PhoneNumberQueryResult;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/")
public class IdentityExampleRestService {

    // Uncomment this example in ***batm-extensions.xml*** and call it for example with:
    // curl -k -XPOST https://localhost:7743/extensions/identity-example/register -d "terminalSerialNumber=BT102239&externalId=EXTID0001&fiatCurrency=USD&limit=1000000&discount=100&phoneNumber=+12065550100&firstName=Chuck&lastName=Norris&emailAddress=chucknorrisfans@hotmail.com&idCardNumber=123456&contactZIP=77868&contactCountry=United States&contactProvince=TX&contactCity=Navasota&contactAddress=4360 Lone Wolf Ranch Road"

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(IdentityExampleRestService.class);
    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    public String register(@FormParam("fiatCurrency") String fiatCurrency, @FormParam("externalId") String externalId,
                           @FormParam("limit") BigDecimal limit, @FormParam("discount") BigDecimal discount,
                           @FormParam("terminalSerialNumber") String terminalSerialNumber, @FormParam("note") String note,
                           @FormParam("phoneNumber") String phoneNumber, @FormParam("firstName") String firstName,
                           @FormParam("lastName") String lastName, @FormParam("emailAddress") String emailAddress,
                           @FormParam("idCardNumber") String idCardNumber, @FormParam("documentValidToYYYYMMDD") String documentValidToYYYYMMDD,
                           @FormParam("contactZIP") String contactZIP,
                           @FormParam("contactCountry") String contactCountry, @FormParam("contactProvince") String contactProvince,
                           @FormParam("contactCity") String contactCity, @FormParam("contactAddress") String contactAddress,
                           @FormParam("selfieImage") String selfieImage, @FormParam("idScan") String idScan) throws ParseException {

        log.info("fiatCurrency   "+ fiatCurrency );
        log.info("externalId   " + externalId);
        log.info("limit   " + limit);
        log.info("discount    " + discount);
        log.info("terminalSerialNumber    " + terminalSerialNumber);
        log.info("note    " + note);
        log.info("phoneNumber     " + phoneNumber);
        log.info("firstName   " + firstName);
        log.info("lastName    " + lastName);
        log.info("emailAddress    " + emailAddress);
        log.info("idCardNumber    " + idCardNumber);
        log.info("documentValidToYYYYMMDD     "+ documentValidToYYYYMMDD);
        log.info("contactZIP      " + contactZIP);
        log.info("contactCountry      " + contactCountry);
        log.info("contactProvince     " + contactProvince);
        log.info("contactCity     " + contactCity);
        log.info("contactAddress     " + contactAddress);


        IExtensionContext ctx = IdentityExampleExtension.getExtensionContext();
        List<ILimit> limits = Collections.singletonList(new LimitExample(fiatCurrency, limit));
        if (phoneNumber.startsWith(" ")){
            phoneNumber = phoneNumber.replaceFirst(" ", "+");
        }
        PhoneNumberQueryResult phoneNumberQueryResult = ctx.queryPhoneNumber(phoneNumber, terminalSerialNumber);

        if (phoneNumberQueryResult.isQuerySuccessful()) {
            if (phoneNumberQueryResult.isLineTypeBlocked()) {
                return "PHONE BLOCKED";
            }
        }

        int state = IIdentity.STATE_NOT_REGISTERED;
        Date now = new Date();

        List<IIdentity> a = ctx.findIdentitiesByDocumentNumber(idCardNumber);
                if (a!= null && !a.isEmpty()){
            return "Error: idCardNumber is already in use ";
        }

        a = ctx.findIdentitiesByPhoneNumber(phoneNumber);
        if (a!= null && !a.isEmpty()){
            return "Error: phoneNumber already in use ";
        }

//        STATE_NOT_REGISTERED = 0;
//        STATE_REGISTERED = 1;
//        STATE_TO_BE_REGISTERED = 2;
//        STATE_PROHIBITED = 3;
//        STATE_ANONYMOUS = 4;
//        STATE_PROHIBITED_TO_BE_REGISTERED   = 5;
        List<IIdentity> identities = new ArrayList<>();
        for (int i=0; i<=5; i++){
            identities.addAll(ctx.findAllIdentitiesByState(i));
        }
        List <IIdentityPiece> identityPieces = identities.stream().flatMap(x -> x.getIdentityPieces().stream()).collect(Collectors.toList());

//        Optional<IIdentityPiece> iIdentityPiece = identityPieces.stream().filter(x -> x.getPhoneNumber().equals(phoneNumber) ||
//            x.getEmailAddress().equalsIgnoreCase(emailAddress) || x.getIdCardNumber().equals(idCardNumber)).findFirst();
//        log.info("identities size: " + String.valueOf(identities.size()));
//        log.info("identityPieces size: " + String.valueOf(identityPieces.size()));

        Optional<IIdentityPiece> iIdentityPiece = identityPieces.stream().filter(x -> x.getEmailAddress()!=null && x.getEmailAddress().equalsIgnoreCase(emailAddress)).findFirst();
        if (iIdentityPiece.isPresent()){
            return "Error: email address already in use";
        }

        IIdentity identity = ctx.addIdentity(fiatCurrency, terminalSerialNumber, externalId, limits, limits, limits, limits, limits, note, state, discount, discount, now, now);
        String identityPublicId = identity.getPublicId();
        ctx.addIdentityPiece(identityPublicId, IdentityPieceExample.fromPersonalInfo(firstName, lastName, idCardNumber, IIdentityPiece.DOCUMENT_TYPE_ID_CARD,
            documentValidToYYYYMMDD == null ? null : new SimpleDateFormat("yyyyMMdd", Locale.US).parse(documentValidToYYYYMMDD),
            contactZIP, contactCountry, contactProvince, contactCity, contactAddress));
        ctx.addIdentityPiece(identityPublicId, IdentityPieceExample.fromPhoneNumber(phoneNumber));
        ctx.addIdentityPiece(identityPublicId, IdentityPieceExample.fromEmailAddress(emailAddress));

        if (selfieImage != null && !selfieImage.isEmpty()){
            //ws may replace "+" with " "
            selfieImage = selfieImage.replaceAll(" " , "+");
            byte[] selfie = Base64.getMimeDecoder().decode(selfieImage);
            ctx.addIdentityPiece(identityPublicId, IdentityPieceExample.fromSelfie("image/jpeg", selfie));
        }
        if (idScan != null && !idScan.isEmpty()){
            //ws may replace "+" with " "
            idScan = idScan.replaceAll(" " , "+");
            byte[] id = Base64.getMimeDecoder().decode(idScan);
            ctx.addIdentityPiece(identityPublicId, IdentityPieceExample.fromIdScan("image/jpeg", id));
        }
        return identityPublicId;
    }

    // curl -k -XPOST https://localhost:7743/extensions/identity-example/update -d "identityPublicId=IE3BVEBUIIXZ3SZV&emailAddress=email@example.com"
    @POST
    @Path("/update")
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@FormParam("identityPublicId") String identityPublicId, @FormParam("emailAddress") String emailAddress) {

        IExtensionContext ctx = IdentityExampleExtension.getExtensionContext();
        IIdentity identity = ctx.findIdentityByIdentityId(identityPublicId);
        if (identity == null) {
            return "identity not found";
        }
        int newState = IIdentity.STATE_REGISTERED;
        String note = identity.getNote() + " updated from an extension";
        IIdentity updatedIdentity = ctx.updateIdentity(identityPublicId, identity.getExternalId(),
            newState, identity.getType(), identity.getCreated(), identity.getRegistered(),
            identity.getVipBuyDiscount(), identity.getVipSellDiscount(), note,
            identity.getLimitCashPerTransaction(), identity.getLimitCashPerHour(), identity.getLimitCashPerDay(), identity.getLimitCashPerWeek(),
            identity.getLimitCashPerMonth(), identity.getLimitCashPer3Months(), identity.getLimitCashPer12Months(), identity.getLimitCashPerCalendarQuarter(),
            identity.getLimitCashPerCalendarYear(), identity.getLimitCashTotalIdentity(), identity.getConfigurationCashCurrency());

        return updatedIdentity.getPublicId();
    }

    // curl -k -XPOST https://localhost:7743/extensions/identity-example/getnotes -d "identityPublicId=IE3BVEBUIIXZ3SZV"
    @POST
    @Path("/getnotes")
    @Produces(MediaType.APPLICATION_JSON)
    public List<IIdentityNote> getNotes(@FormParam("identityPublicId") String identityPublicId) {

        IExtensionContext ctx = IdentityExampleExtension.getExtensionContext();
        IIdentity identity = ctx.findIdentityByIdentityId(identityPublicId);
        if (identity == null) {
            return new ArrayList<>();
        }

        return identity.getNotes();
    }
}
