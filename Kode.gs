// StokQu - Inventory System | Author: Lintang Coding by Kevin Ricky Utama, S.Kom.

function doGet(e) {
  var action = e.parameter.action;
  if (action === "getItems") {
    return ContentService.createTextOutput(JSON.stringify(getItems()))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function doPost(e) {
  try {
    var postData = JSON.parse(e.postData.contents);
    var action = postData.action;
    if (action === "syncItems") {
      return ContentService.createTextOutput(JSON.stringify({ success: true }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    if (action === "addLoan") {
      return ContentService.createTextOutput(JSON.stringify({ success: true }))
        .setMimeType(ContentService.MimeType.JSON);
    }
    if (action === "returnLoan") {
      return ContentService.createTextOutput(JSON.stringify({ success: true }))
        .setMimeType(ContentService.MimeType.JSON);
    }
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ success: false, error: err.message }))
      .setMimeType(ContentService.MimeType.JSON);
  }
  return ContentService.createTextOutput(JSON.stringify({ success: false, error: "Invalid Action" }))
    .setMimeType(ContentService.MimeType.JSON);
}

function getItems() {
  return [
    { idBarang: "AL001", namaBarang: "Laptop ASUS", stokAwal: 5 },
    { idBarang: "BH001", namaBarang: "Kertas A4", stokAwal: 100 }
  ];
}
