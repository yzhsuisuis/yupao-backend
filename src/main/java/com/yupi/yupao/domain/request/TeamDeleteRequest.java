package com.yupi.yupao.domain.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class TeamDeleteRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private Long teamId;
}
