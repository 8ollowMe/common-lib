package com.followMe.common.event.exception;

import com.followMe.common.exception.BusinessException;
import com.followMe.common.exception.CommonErrorCode;

public class EventPublishFailureEvent extends BusinessException {
  public EventPublishFailureEvent(String message) {
    super(CommonErrorCode.EVENT_PUBLISH_FAILURE, message);
  }
}
