import requests
import json


def sub_string(start, end, source):
  try:
    if end:
      return source[source.index(start,
                                 0,
                                 len(source)) + len(start):source.index(end,
                                                                        source.index(start,
                                                                                     0,
                                                                                     len(source)) + len(start),
                                                                        len(source))]
    else:
      return source[source.index(start, 0, len(source)) + len(start):]
  except:
    print("==========The expected results were not found! ============")
    raise

def get_signature(url):

  


  return response.text

def get_uid(username):
  session = requests.session()
  session.get("https://www.tiktok.com/")
  url = "https://www.tiktok.com/@" + username

  response = session.get(url).text

  uid = sub_string('profile/','?',response)
  return uid



def comment_mention(video_url,username,text):

    """
    :param video_url: witch video you want to comment
    :param username: witch user you want to @
    :param text: comments content
    :return: comment status 7->success ;2-> not success;
    """

    video_url = video_url+"?"
    aweme_id = sub_string('video/','?',video_url)
    uid = get_uid(username)
    url = "https://www.tiktok.com/api/comment/publish/?aid=1988&app_name=tiktok_web&aweme_id=" + aweme_id + "&text=%40" + username + "+" + text.replace("+","%2B").replace(" ","+") + "&text_extra=[%7B%22start%22:0,%22end%22:" + str(len(username) + 1) + ",%22user_id%22:%22" + uid + "%22,%22type%22:3%7D]"

    _signature = get_signature(url)

    all_url = url + "&_signature=" + _signature

    payload = {}

    headers = {
        'Host': 'www.tiktok.com',
        'Connection': 'keep-alive',
        'Accept': 'application/json, text/plain, */*',
        'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.75 Safari/537.36',
        'Referer': 'https://www.tiktok.com/',
        'Accept-Language': 'zh-CN,zh;q=0.9',
        'Cookie': cookie
    }

    response = requests.request("POST", all_url, headers=headers, data=payload, verify=False)

    print(response.text)

    status = json.loads(response.text)['comment']['status']

    return status


cookie = ''



if __name__ == '__main__':

    video_url = "https://www.tiktok.com/@so_min_ee/video/6950675940280126722"
    mention_username = "magi0225"
    text = "look at this!!!"

    ret_status = comment_mention(video_url,mention_username,text)

    print("status",ret_status)





