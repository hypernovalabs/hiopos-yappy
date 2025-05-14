//// File: AlternativePaymentMock.kt
//package com.tefbanesco
//
///**
// * Modo mock para pruebas. Simula todo el flujo de Yappy sin tocar la red.
// *
// * Usa los valores de configuración de UAT de pruebas:
// *   endpoint    = "https://api-integrationcheckout-uat.yappycloud.com/v1"
// *   apiKey      = "937bfbe7-a29e-4fcb-b155-affe133bd3a6"
// *   secretKey   = "WVBfQkVCOTZDNzQtMDgxOC0zODVBLTg0ODktNUQxQTNBODVCRjFF"
// *   device.id   = "CAJA-02"
// *   device.name = "C"
// *   device.user = "a"
// *   groupId     = "ID-TESTING-HYPER"
// */
//class AlternativePaymentMock : AlternativePayment(
//    apiKey        = "937bfbe7-a29e-4fcb-b155-affe133bd3a6",
//    secretKey     = "WVBfQkVCOTZDNzQtMDgxOC0zODVBLTg0ODktNUQxQTNBODVCRjFF",
//    baseUrl       = "https://api-integrationcheckout-uat.yappycloud.com/v1",
//    defaultDevice = DeviceInfo(
//        id   = "CAJA-02",
//        name = "C",
//        user = "a"
//    ),
//    defaultGroupId= "ID-TESTING-HYPER",
//    useMock       = true
//) {
//    // Aquí podrías sobreescribir comportamientos si lo necesitas,
//    // pero con useMock = true ya se simula todo el flujo.
//}
