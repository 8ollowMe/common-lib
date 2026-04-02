package com.followMe.common.event.exception;

import com.followMe.common.exception.BusinessException;
import com.followMe.common.exception.CommonErrorCode;

public class EventPublishFailureException extends BusinessException {
  public EventPublishFailureException(String message) {
    super(CommonErrorCode.EVENT_PUBLISH_FAILURE, message);
  }
}
