package com.smartfinance.dto.response;

import java.util.List;

public  record StatementPreviewResponse(int filesReceived, int filesReadable, List<FilePreviewDTO> files) {

}
