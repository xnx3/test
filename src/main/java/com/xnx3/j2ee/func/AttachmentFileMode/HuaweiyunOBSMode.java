package com.xnx3.j2ee.func.AttachmentFileMode;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.obs.services.exception.ObsException;
import com.obs.services.model.PutObjectResult;
import com.xnx3.j2ee.Global;
import com.xnx3.j2ee.func.AttachmentFileMode.hander.OBSHandler;
import com.xnx3.j2ee.vo.UploadFileVO;

/**
 * 附件上传之 华为云 OBS 
 * @author 管雷鸣
 *
 */
public class HuaweiyunOBSMode implements StorageModeInterface{
	private static OBSHandler obsHandler;	//禁用，通过getObsUtil() 获取
	private static String obsBucketName; // 当前进行操作桶的名称
	
	
	
	/**
	 * 获取华为云OBS的操作类
	 * @author 李鑫
	 * @return 当前华为云OBS的操作类型
	 */
	public static OBSHandler getObsHander() {
		if(obsHandler == null) {
			obsHandler = new OBSHandler(Global.get("HUAWEIYUN_ACCESSKEYID"),Global.get("HUAWEIYUN_ACCESSKEYSECRET"),Global.get("HUAWEIYUN_OBS_ENDPOINT"));
			// 如果设置过CDN的路径测设置为CDN路径，没有设置则为桶原生的访问路径
			obsHandler.setUrlForCDN(Global.get("ATTACHMENT_FILE_URL"));
			// 在数据库中读取进行操作的桶的明恒
			obsHandler.setObsBucketName(Global.get("HUAWEIYUN_OBS_BUCKETNAME"));
			// 对桶名称进行当前类内缓存
			obsBucketName = obsHandler.getObsBucketName();
		}
		return obsHandler;
	}
	
	/**
	 * 上传字符串到OBS为文件
	 * @author 李鑫
	 * @param path 存储文件的路径和名称 例："site/1.txt"
	 * @param text 上传的字符串文本信息
	 * @param encode 上传的编码格式 例："UTF-8"
	 */
	@Override
	public void putStringFile(String path, String text, String encode) {
		try {
			getObsHander().putStringFile(obsBucketName, path, text, encode);
		} catch (ObsException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 上传本地文件到OBS
	 * @author 李鑫
	 * @param filePath 上传文件路径和名称 例："site/1.txt"
	 * @param localFile 需要上传的本地文件
	 * @return {@link com.xnx3.j2ee.vo.UploadFileVO} result 1: 成功；0 失败。
	 */
	@Override
	public UploadFileVO put(String filePath, File localFile) {
		// 上传文件
		PutObjectResult result = getObsHander().putLocalFile(obsBucketName, filePath, localFile);
		// 获取封装类
		return getUploadFileVO(result);
	}
	
	/**
	 * 通过流进行上传文件
	 * @author 李鑫
	 * @param path 上传文件路径和名称 例："site/1.txt"
	 * @param inputStream 需要上传文件的输入流
	 * @return {@link com.xnx3.j2ee.vo.UploadFileVO} result 1: 成功；0 失败。
	 */
	@Override
	public UploadFileVO put(String path, InputStream inputStream) {
		// 上传文件
		PutObjectResult result = getObsHander().putFileByStream(obsBucketName, path, inputStream);
		// 获取封装类
		return getUploadFileVO(result);
	}
	
	/**
	 * 获取指定文件的文本内容
	 * @author 李鑫
	 * @param path 进行操作的文件路径 例:"site/1.txt"
	 * @return 文件的文本内容
	 */
	@Override
	public String getTextByPath(String path) {
		String content = null;
		try {
			content = new String(getObsHander().getFileByteArray(obsBucketName, path), "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	/**
	 * 删除文件
	 * @author 李鑫
	 * @param filePath 需要删除的文件路径加名称 例："site/1.sh"
	 */
	@Override
	public void deleteObject(String filePath) {
		getObsHander().deleteObject(obsBucketName, filePath);
	}
	
	@Override
	public void putForUEditor(String filePath, InputStream input, ObjectMetadata meta) {
		com.obs.services.model.ObjectMetadata metaData = new com.obs.services.model.ObjectMetadata();
		getObsHander().putFilebyInstreamAndMeta(obsBucketName, filePath, input, metaData);
	}
	
	/**
	 * 获得指定路径下的对象个数
	 * @author 李鑫
	 * @param path 指定查询的文件夹路径 例：“site/”
	 * @return 
	 */
	@Override
	public long getDirectorySize(String path) {
		return getObsHander().getFolderObjectsSize(obsBucketName, path);
	}
	
	/**
	 * OBS内对象复制
	 * @author 李鑫
	 * @param originalFilePath 源文件的路径和文件名 例："site/2010/example.txt"
	 * @param newFilePath 目标文件的路径和文件名 例："site/2010/example_bak.txt"
	 */
	@Override
	public void copyObject(String originalFilePath, String newFilePath) {
		getObsHander().copyObject(obsBucketName, originalFilePath, obsBucketName, newFilePath);
	}
	
	/**
	 * 将PutObjectResult封装为UploadFileVO但会返回
	 * @author 李鑫
	 * @param result 文件上传返回的结果封装类
	 * @return {@link com.xnx3.j2ee.vo.UploadFileVO} 经过封装的UploadFileVO类
	 */
	private UploadFileVO getUploadFileVO(PutObjectResult result) {
		UploadFileVO vo = new UploadFileVO();
		// 上传成功
		if(result.getStatusCode() == 200) {
			vo.setResult(UploadFileVO.SUCCESS);
			vo.setInfo("success");
			vo.setPath(result.getObjectKey());
			vo.setUrl(result.getObjectUrl());
			return vo;
		}
		// 上传失败
		vo.setBaseVO(UploadFileVO.FAILURE, "上传失败");
		return vo;
	}

}
